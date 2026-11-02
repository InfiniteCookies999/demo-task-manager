package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Board;
import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.CategoryColor;
import infinitecookies.task.manager.models.dto.CategoryReorderRequest;
import infinitecookies.task.manager.models.dto.CategoryResponse;
import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.models.websock.CategoriesReorderedEvent;
import infinitecookies.task.manager.models.websock.CategoryCreatedEvent;
import infinitecookies.task.manager.models.websock.CategoryDeletedEvent;
import infinitecookies.task.manager.models.websock.CategoryUpdatedEvent;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTests {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        // Manually setup mocks to test authorization at the boardService level.
        BoardService boardService = new BoardService(
            boardRepository,
            applicationEventPublisher
        );

        categoryService = new CategoryService(
            categoryRepository,
            boardRepository,
            boardService,
            applicationEventPublisher
        );
    }

    @Test
    void createCategoryLacksBoardAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> categoryService.createCategory(1L, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(categoryRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createCategoryForTooManyCategories() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.countByBoardId(1L)).thenReturn((long)Board.MAX_CATEGORIES);

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> categoryService.createCategory(1L, 1L));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(categoryRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createCategorySuccessful() {
        Board board = new Board();

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.countByBoardId(1L)).thenReturn(2L);
        when(boardRepository.getReferenceById(1L)).thenReturn(board);

        CategoryResponse response = categoryService.createCategory(1L, 1L);

        assertNotNull(response);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());

        Category savedCategory = categoryCaptor.getValue();
        assertEquals(board, savedCategory.getBoard());
        assertEquals(2, savedCategory.getPosition());

        verify(applicationEventPublisher).publishEvent(any(CategoryCreatedEvent.class));
    }

    @Test
    void getCategoriesLacksBoardAuthorization() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> categoryService.getCategories(1L, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(categoryRepository, never()).findAllByBoardIdOrderByPositionAsc(any());
    }

    @Test
    void updateCategoryLacksBoardAuthorization() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        CategoryUpdateRequest request = mock(CategoryUpdateRequest.class);
        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> categoryService.updateCategory(1L, 1L, 1L, request));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(categoryRepository, never()).findByIdAndBoardId(any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateCategorySuccessful() {
        Category category = new Category();

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findByIdAndBoardId(1L, 1L)).thenReturn(Optional.of(category));

        CategoryUpdateRequest request = new CategoryUpdateRequest("Unfinished", CategoryColor.GREEN);
        categoryService.updateCategory(1L, 1L, 1L, request);

        assertEquals("Unfinished", category.getName());
        assertEquals(CategoryColor.GREEN, category.getColor());

        verify(applicationEventPublisher).publishEvent(any(CategoryUpdatedEvent.class));
    }

    @Test
    void reorderCategoriesLacksBoardAuthorization() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        CategoryReorderRequest request = mock(CategoryReorderRequest.class);
        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> categoryService.reorderCategories(1L, 1L, request));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(categoryRepository, never()).findAllByBoardId(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderCategoriesMissingCategoryIds() {
        List<Category> categories = List.of(
            new Category(),
            new Category(),
            new Category()
        );

        CategoryReorderRequest request = new CategoryReorderRequest(List.of(1L, 2L));

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findAllByBoardId(1L)).thenReturn(categories);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> categoryService.reorderCategories(1L, 1L, request));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderCategoriesContainsInvalidCategoryId() {
        Category category1 = new Category();
        category1.setId(1L);

        Category category2 = new Category();
        category2.setId(2L);

        CategoryReorderRequest request = new CategoryReorderRequest(List.of(1L, 999L));

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findAllByBoardId(1L)).thenReturn(List.of(category1, category2));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> categoryService.reorderCategories(1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("One or more categories not found in board", e.getReason());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderCategoriesContainsDuplicateIds() {
        Category category1 = new Category();
        category1.setId(1L);

        Category category2 = new Category();
        category2.setId(2L);

        Category category3 = new Category();
        category3.setId(3L);

        CategoryReorderRequest request = new CategoryReorderRequest(List.of(1L, 1L, 2L));

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findAllByBoardId(1L)).thenReturn(List.of(category1, category2, category3));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> categoryService.reorderCategories(1L, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("Duplicate category IDs are not allowed", e.getReason());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reorderCategoriesSuccessful() {
        Category category1 = new Category();
        category1.setId(1L);
        category1.setPosition(0);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setPosition(1);

        Category category3 = new Category();
        category3.setId(3L);
        category3.setPosition(2);

        CategoryReorderRequest request = new CategoryReorderRequest(List.of(3L, 2L, 1L));

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.findAllByBoardId(1L)).thenReturn(List.of(category1, category2, category3));

        categoryService.reorderCategories(1L, 1L, request);

        assertEquals(2, category1.getPosition());
        assertEquals(1, category2.getPosition());
        assertEquals(0, category3.getPosition());

        verify(applicationEventPublisher).publishEvent(any(CategoriesReorderedEvent.class));
    }

    @Test
    void deleteCategoryLacksBoardAuthorization() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> categoryService.deleteCategory(1L, 1L, 1L));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(categoryRepository, never()).deleteByIdAndBoardId(any(), any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteCategoryNotFound() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.deleteByIdAndBoardId(1L, 1L)).thenReturn(0);

        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> categoryService.deleteCategory(1L, 1L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteCategorySuccessful() {
        Long boardId = 1L;
        Long deleteCategoryId = 2L;

        // Remaining categories.
        Category category1 = new Category();
        category1.setId(1L);
        category1.setPosition(0);

        Category category2 = new Category();
        category2.setId(3L);
        category2.setPosition(2);

        Category category3 = new Category();
        category3.setId(4L);
        category3.setPosition(3);

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(categoryRepository.deleteByIdAndBoardId(deleteCategoryId, boardId)).thenReturn(1);
        when(categoryRepository.findAllByBoardIdOrderByPositionAsc(1L))
            .thenReturn(List.of(category1, category2, category3));

        categoryService.deleteCategory(1L, 1L, deleteCategoryId);

        verify(categoryRepository).deleteByIdAndBoardId(deleteCategoryId, boardId);
        verify(categoryRepository).flush();

        // Verify positions are normalized.
        assertEquals(0, category1.getPosition());
        assertEquals(1, category2.getPosition());
        assertEquals(2, category3.getPosition());

        verify(applicationEventPublisher).publishEvent(any(CategoryDeletedEvent.class));
    }
}
