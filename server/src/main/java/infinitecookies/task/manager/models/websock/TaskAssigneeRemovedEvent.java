package infinitecookies.task.manager.models.websock;

public record TaskAssigneeRemovedEvent(
    Long boardId,
    Long categoryId,
    Long taskId,
    Long assignedUserId
) {}
