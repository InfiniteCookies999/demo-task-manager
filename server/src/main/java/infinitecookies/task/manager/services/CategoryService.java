package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.CategoryColor;
import infinitecookies.task.manager.models.dto.CategoryReorderRequest;
import infinitecookies.task.manager.models.dto.CategoryResponse;
import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.repositories.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public CategoryResponse createCategory(String sessionId) {

        // This is fine the user would have to create/delete over 2 billion times.
        int nextPosition = (int) repository.countBySessionId(sessionId);

        Category category = new Category();
        category.setName("Untitled");
        category.setColor(CategoryColor.GRAY);
        category.setSessionId(sessionId);
        category.setPosition(nextPosition);
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
        // TODO (maddie): better error handling.
        Category category = repository.findByIdAndSessionId(id, sessionId).orElseThrow();

        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.color() != null) {
            category.setColor(request.color());
        }

        repository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id, String sessionId) {
        repository.deleteByIdAndSessionId(id, sessionId);
    }

    @Transactional
    public void reorderCategories(String sessionId, CategoryReorderRequest request) {
        // TODO (maddie): better error handling.

        List<CategoryReorderRequest.Entry> entries = request.entries();

        int previousPosition = -1;
        Set<Long> ids = new HashSet<>();
        for (CategoryReorderRequest.Entry entry : entries) {
            if (!ids.add(entry.id())) {
                throw new IllegalArgumentException("Duplicate category ID");
            }
            if (entry.position() < 0) {
                throw new IllegalArgumentException("Positions must be non-negative");
            }

            // Positions must be strictly increasing
            if (entry.position() <= previousPosition) {
                throw new IllegalArgumentException(
                    "Category positions must be strictly increasing"
                );
            }
            previousPosition = entry.position();

            Category category = repository.findByIdAndSessionId(entry.id(), sessionId)
                .orElseThrow();
            category.setPosition(entry.position());
        }
    }
}
