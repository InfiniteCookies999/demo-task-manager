package infinitecookies.task.manager.models.websock;

public record TaskAssigneeAddedEvent(
    Long boardId,
    Long categoryId,
    Long taskId,
    Long assignedUserId
) {}
