package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.CategoryReorderRequest;
import infinitecookies.task.manager.models.dto.CategoryResponse;
import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.services.CategoryService;
import infinitecookies.task.manager.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/categories")
public class CategoriesController {

    private final CategoryService service;

    public CategoriesController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    public CategoryResponse createCategory(@PathVariable Long boardId, SessionUser sessionUser) {
        return service.createCategory(sessionUser.id(), boardId);
    }

    @PatchMapping("/{id}")
    public void updateCategory(@PathVariable Long boardId, @PathVariable Long id,
                               SessionUser sessionUser,
                               @RequestBody CategoryUpdateRequest request) {
        service.updateCategory(sessionUser.id(), boardId, id, request);
    }

    @PatchMapping("/reorder")
    public void reorderCategories(@PathVariable Long boardId,
                                  SessionUser sessionUser,
                                  @RequestBody CategoryReorderRequest request) {
        service.reorderCategories(sessionUser.id(), boardId, request);
    }

    @GetMapping
    public List<CategoryResponse> getCategories(@PathVariable Long boardId, SessionUser sessionUser) {
        return service.getCategories(sessionUser.id(), boardId);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long boardId, @PathVariable Long id, SessionUser session) {
        service.deleteCategory(session.id(), boardId, id);
    }
}
