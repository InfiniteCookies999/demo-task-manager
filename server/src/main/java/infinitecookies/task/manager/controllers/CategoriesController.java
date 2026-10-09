package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.CategoryReorderRequest;
import infinitecookies.task.manager.models.dto.CategoryResponse;
import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.services.CategoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoriesController {

    private final CategoryService service;

    public CategoriesController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    public CategoryResponse createCategory(HttpSession session) {
        return service.createCategory(session.getId());
    }

    @PatchMapping("/{id}")
    public void updateCategory(@PathVariable Long id, HttpSession session,
                               @RequestBody CategoryUpdateRequest request) {
        service.updateCategory(id, session.getId(), request);
    }

    @PatchMapping("/reorder")
    public void reorderCategories(HttpSession session, @RequestBody CategoryReorderRequest request) {
        service.reorderCategories(session.getId(), request);
    }

    @GetMapping
    public List<CategoryResponse> getCategories(HttpSession session) {
        return service.getCategories(session.getId());
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id, HttpSession session) {
        service.deleteCategory(id, session.getId());
    }
}
