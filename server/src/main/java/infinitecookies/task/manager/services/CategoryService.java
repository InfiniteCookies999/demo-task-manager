package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.CategoryColor;
import infinitecookies.task.manager.models.dto.CategoryReorderRequest;
import infinitecookies.task.manager.models.dto.CategoryResponse;
import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.repositories.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {

    private static final long MAX_CATEGORIES = 30;

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public CategoryResponse createCategory(String sessionId) {

        long count = repository.countBySessionId(sessionId);
        if (count >= MAX_CATEGORIES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hit category limit");
        }

        Category category = new Category();
        category.setName("Untitled");
        category.setColor(CategoryColor.GRAY);
        category.setSessionId(sessionId);
        category.setPosition((int) count);
        repository.save(category);
        return CategoryResponse.fromCategory(category);
    }

    public List<CategoryResponse> getCategories(String sessionId) {
        List<Category> categories = repository.findAllBySessionIdOrderByPositionAsc(sessionId);
        return categories.stream()
            .map(CategoryResponse::fromCategory)
            .toList();
    }

    @Transactional
    public void updateCategory(Long id, String sessionId, CategoryUpdateRequest request) {

        Category category = repository.findByIdAndSessionId(id, sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.color() != null) {
            category.setColor(request.color());
        }

        repository.save(category);
    }

    @Transactional
    public void reorderCategories(String sessionId, CategoryReorderRequest request) {

        List<CategoryReorderRequest.Entry> entries = request.entries();

        List<Category> categories = new ArrayList<>();
        for (CategoryReorderRequest.Entry entry : entries) {
            Category category = repository.findByIdAndSessionId(entry.id(), sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            categories.add(category);
        }

        long count = repository.countBySessionId(sessionId);
        if (categories.size() != count) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must specify the re-ordering for all categores");
        }

        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setPosition(i);
        }
    }

    @Transactional
    public void deleteCategory(Long id, String sessionId) {
        int deleted = repository.deleteByIdAndSessionId(id, sessionId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }

        // Re-normalizing the positions.
        List<Category> categories = repository.findAllBySessionIdOrderByPositionAsc(sessionId);
        for (int i= 0; i < categories.size(); i++) {
            categories.get(i).setPosition(i);
        }
    }
}
