package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.User;
import infinitecookies.task.manager.models.dto.*;
import infinitecookies.task.manager.services.TaskService;
import infinitecookies.task.manager.session.SessionUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @GetMapping("/{id}/assignees")
    public List<TaskAssigneeResponse> getTaskAssignees(@PathVariable Long boardId, @PathVariable Long id,
                                                       SessionUser sessionUser) {
        return service.getTaskAssignees(sessionUser.id(), boardId, id);
    }

    @GetMapping("/{id}/search-assignees")
    public List<TaskAssigneeSearchResponse> searchAssigneesForTask(
        @PathVariable Long boardId,
        @PathVariable Long id,
        SessionUser sessionUser,

        @RequestParam
        @Valid
        @NotBlank
        @Size(min = 2, max = User.MAX_USERNAME_LENGTH)
        String username
    ) {
        return service.searchAssignees(sessionUser.id(), boardId, id, username);
    }

//    @PutMapping("/{taskId}/assignees/{assignedUserId}")
//    public void assignToTask(
//        @PathVariable Long boardId,
//        @PathVariable Long taskId,
//        @PathVariable Long assignedUserId,
//        SessionUser sessionUser
//    ) {
//        service.assignToTask(sessionUser.id(), boardId, taskId, assignedUserId);
//    }
//
//    @DeleteMapping("/{taskId}/assignees/{assignedUserId}")
//    public void unassignFromTask(
//        @PathVariable Long boardId,
//        @PathVariable Long taskId,
//        @PathVariable Long assignedUserId,
//        SessionUser sessionUser
//    ) {
//        service.unassignFromTask(sessionUser.id(), boardId, taskId, assignedUserId);
//    }

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
