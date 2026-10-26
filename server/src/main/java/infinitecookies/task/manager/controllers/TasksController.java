package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.*;
import infinitecookies.task.manager.services.TaskService;
import infinitecookies.task.manager.session.SessionUser;
import jakarta.validation.Valid;
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
                                   @Valid @RequestBody CreateTaskRequest request) {
        return service.createTask(sessionUser.id(), boardId, request);
    }

    @GetMapping
    public List<TaskResponse> getTasks(@PathVariable Long boardId, SessionUser sessionUser) {
        return service.getTasks(sessionUser.id(), boardId);
    }

    @PatchMapping("/{id}")
    public void updateTask(@PathVariable Long boardId, @PathVariable Long id,
                           SessionUser sessionUser,
                           @Valid @RequestBody TaskUpdateRequest request) {
        service.updateTask(sessionUser.id(), boardId, id, request);
    }

    @PatchMapping("/reorder")
    public void reorderTasks(@PathVariable Long boardId,
                             SessionUser sessionUser,
                             @Valid @RequestBody TaskReorderRequest request) {
        service.reorderTasks(sessionUser.id(), boardId, request);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long boardId, @PathVariable Long id, SessionUser sessionUser) {
        service.deleteTask(sessionUser.id(), boardId, id);
    }
}
