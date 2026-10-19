package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.CreateTaskRequest;
import infinitecookies.task.manager.models.dto.TaskResponse;
import infinitecookies.task.manager.models.dto.TaskUpdateRequest;
import infinitecookies.task.manager.services.TaskService;
import infinitecookies.task.manager.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/tasks")
public class TasksController {

    private final TaskService service;

    public TasksController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    public TaskResponse createTask(@PathVariable Long boardId, SessionUser sessionUser,
                                   @RequestBody CreateTaskRequest request) {
        return service.createTask(sessionUser.id(), boardId, request);
    }

    @GetMapping
    public List<TaskResponse> getTasks(@PathVariable Long boardId, SessionUser sessionUser,
                                       @RequestParam Long categoryId) {
        return service.getTasksForCategory(sessionUser.id(), boardId, categoryId);
    }

    @PatchMapping("/{id}")
    public void updateTask(@PathVariable Long boardId, @PathVariable Long id,
                           SessionUser sessionUser,
                           @RequestBody TaskUpdateRequest request) {
        service.updateTask(sessionUser.id(), boardId, id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long boardId, @PathVariable Long id, SessionUser sessionUser) {
        service.deleteTask(sessionUser.id(), boardId, id);
    }
}
