package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.CategoryUpdateRequest;
import infinitecookies.task.manager.models.dto.CreateTaskRequest;
import infinitecookies.task.manager.models.dto.TaskResponse;
import infinitecookies.task.manager.models.dto.TaskUpdateRequest;
import infinitecookies.task.manager.services.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TasksController {

    private final TaskService service;

    public TasksController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    public TaskResponse createTask(HttpSession session, @RequestBody CreateTaskRequest request) {
        return service.createTask(session.getId(), request);
    }

    @GetMapping
    public List<TaskResponse> getTasks(HttpSession session, @RequestParam Long categoryId) {
        return service.getTasksForCategory(session.getId(), categoryId);
    }

    @PatchMapping("/{id}")
    public void updateTask(@PathVariable Long id, HttpSession session,
                               @RequestBody TaskUpdateRequest request) {
        service.updateTask(id, session.getId(), request);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id, HttpSession session) {
        service.deleteTask(id, session.getId());
    }
}
