package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.CategoryColor;
import infinitecookies.task.manager.models.Task;
import infinitecookies.task.manager.models.dto.CategoryReorderRequest;
import infinitecookies.task.manager.models.dto.CategoryResponse;
import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.models.websock.CategoriesReorderedEvent;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BoardRepository boardRepository;
    private final BoardService boardService;

    private final ApplicationEventPublisher applicationEventPublisher;

    public CategoryService(CategoryRepository categoryRepository,
                           BoardRepository boardRepository,
                           BoardService boardService,
                           ApplicationEventPublisher applicationEventPublisher) {
        this.categoryRepository = categoryRepository;
        this.boardRepository = boardRepository;
        this.boardService = boardService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public CategoryResponse createCategory(Long userId, Long boardId) {
        boardService.checkHasAuthorization(userId, boardId);

        long count = categoryRepository.countByBoardId(boardId);

        Category category = new Category();
        category.setName("Untitled");
        category.setColor(CategoryColor.GRAY);
        category.setBoard(boardRepository.getReferenceById(boardId));
        category.setPosition((int) count);
        categoryRepository.save(category);
        return CategoryResponse.fromCategory(category);
    }

    public List<CategoryResponse> getCategories(Long userId, Long boardId) {
        boardService.checkHasAuthorization(userId, boardId);

        List<Category> categories = categoryRepository.findAllByBoardIdOrderByPositionAsc(boardId);
        return categories.stream()
            .map(CategoryResponse::fromCategory)
            .toList();
    }

    @Transactional
    public void updateCategory(Long userId, Long boardId, Long id, CategoryUpdateRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        Category category = categoryRepository.findByIdAndBoardId(id, boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.color() != null) {
            category.setColor(request.color());
        }
    }

    @Transactional
    public void reorderCategories(Long userId, Long boardId, CategoryReorderRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        // TODO (maddie): its unclear if this logic catches duplicates. When we write tests we
        //                should explicitly check for that.

        List<Category> categories = categoryRepository.findAllByBoardId(boardId);
        if (categories.size() != request.categoryIds().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Must specify the re-ordering for all categories");
        }

        // Reorder the categories to match the request order so the positions may be updated
        // correctly.
        Map<Long, Category> categoriesById = categories.stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Category> orderedCategories = new ArrayList<>();
        for (Long categoryId : request.categoryIds()) {
            Category category = categoriesById.get(categoryId);

            if (category == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "One or more categories not found in board");
            }

            orderedCategories.add(category);
        }

        // Update positions in the requested order.
        for (int i = 0; i < orderedCategories.size(); i++) {
            orderedCategories.get(i).setPosition(i);
        }

        applicationEventPublisher.publishEvent(
            new CategoriesReorderedEvent(boardId, request.categoryIds()));
    }

    @Transactional
    public void deleteCategory(Long userId, Long boardId, Long id) {
        boardService.checkHasAuthorization(userId, boardId);

        int deleted = categoryRepository.deleteByIdAndBoardId(id, boardId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        categoryRepository.flush();

        // Re-normalizing the positions.
        List<Category> categories = categoryRepository.findAllByBoardIdOrderByPositionAsc(boardId);
        for (int i= 0; i < categories.size(); i++) {
            categories.get(i).setPosition(i);
        }
    }
}
