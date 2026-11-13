package infinitecookies.task.manager.services;

import infinitecookies.task.manager.exceptions.ApiErrorCode;
import infinitecookies.task.manager.exceptions.ApiException;
import infinitecookies.task.manager.models.dto.CategoryListResponse;
import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.Category;
import infinitecookies.task.manager.models.dto.CategoryReorderRequest;
import infinitecookies.task.manager.models.dto.CategoryResponse;
import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.models.websock.CategoriesReorderedEvent;
import infinitecookies.task.manager.models.websock.CategoryCreatedEvent;
import infinitecookies.task.manager.models.websock.CategoryDeletedEvent;
import infinitecookies.task.manager.models.websock.CategoryUpdatedEvent;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.CategoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BoardRepository boardRepository;
    private final BoardService boardService;
    private final BoardRevisionService boardRevisionService;

    private final ApplicationEventPublisher applicationEventPublisher;

    public CategoryService(CategoryRepository categoryRepository,
                           BoardRepository boardRepository,
                           BoardService boardService,
                           BoardRevisionService boardRevisionService,
                           ApplicationEventPublisher applicationEventPublisher) {
        this.categoryRepository = categoryRepository;
        this.boardRepository = boardRepository;
        this.boardService = boardService;
        this.boardRevisionService = boardRevisionService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public CategoryResponse createCategory(Long userId, Long boardId) {
        boardService.checkHasAuthorization(userId, boardId);

        long nextPosition = categoryRepository.countByBoardId(boardId);

        if (nextPosition >= Board.MAX_CATEGORIES) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                ApiErrorCode.CATEGORY_LIMIT_EXCEEDED,
                "A board cannot have more than " + Board.MAX_CATEGORIES + " categories"
            );
        }

        Category category = new Category();
        category.setBoard(boardRepository.getReferenceById(boardId));
        category.setPosition((int) nextPosition);
        categoryRepository.save(category);

        long revision = boardRevisionService.incrementRevision(boardId);
        CategoryResponse response = CategoryResponse.fromCategory(category);
        applicationEventPublisher.publishEvent(new CategoryCreatedEvent(boardId, revision, response));

        return response;
    }

    public CategoryListResponse getCategories(Long userId, Long boardId) {
        boardService.checkHasAuthorization(userId, boardId);

        long revision = boardRevisionService.getRevision(boardId);

        List<Category> categories = categoryRepository.findAllByBoardIdOrderByPositionAsc(boardId);

        List<CategoryResponse> responseCategories = categories.stream()
            .map(CategoryResponse::fromCategory)
            .toList();
        return new CategoryListResponse(revision, responseCategories);
    }

    @Transactional
    public void updateCategory(Long userId, Long boardId, Long id, CategoryUpdateRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        Category category = categoryRepository.findByIdAndBoardId(id, boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.CATEGORY_NOT_FOUND, "Category not found"
            ));

        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.color() != null) {
            category.setColor(request.color());
        }

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(
            new CategoryUpdatedEvent(boardId, revision, id, request.name(), request.color()));
    }

    @Transactional
    public void reorderCategories(Long userId, Long boardId, CategoryReorderRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        List<Category> categories = categoryRepository.findAllByBoardId(boardId);
        if (categories.size() != request.categoryIds().size()) {
            throw new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.CATEGORY_REORDER_MISSING_CATEGORY,
                "Must specify the re-ordering for all categories"
            );
        }

        // Check for duplicates.
        Set<Long> uniqueIds = new HashSet<>(request.categoryIds());
        if (uniqueIds.size() != request.categoryIds().size()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, ApiErrorCode.CATEGORY_DUPLICATE_ID,
                "Duplicate category IDs are not allowed");
        }

        // Reorder the categories to match the request order so the positions may be updated
        // correctly.
        Map<Long, Category> categoriesById = categories.stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Category> orderedCategories = new ArrayList<>();
        for (Long categoryId : request.categoryIds()) {
            Category category = categoriesById.get(categoryId);

            if (category == null) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST, ApiErrorCode.CATEGORY_NOT_FOUND,
                    "One or more categories not found in board"
                );
            }

            orderedCategories.add(category);
        }

        // Update positions in the requested order.
        for (int i = 0; i < orderedCategories.size(); i++) {
            orderedCategories.get(i).setPosition(i);
        }

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(
            new CategoriesReorderedEvent(boardId, revision, request.categoryIds()));
    }

    @Transactional
    public void deleteCategory(Long userId, Long boardId, Long id) {
        boardService.checkHasAuthorization(userId, boardId);

        int deleted = categoryRepository.deleteByIdAndBoardId(id, boardId);
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.CATEGORY_NOT_FOUND, "Category not found");
        }
        categoryRepository.flush();

        // Re-normalizing the positions.
        List<Category> categories = categoryRepository.findAllByBoardIdOrderByPositionAsc(boardId);
        for (int i= 0; i < categories.size(); i++) {
            categories.get(i).setPosition(i);
        }

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(new CategoryDeletedEvent(boardId, revision, id));
    }
}
