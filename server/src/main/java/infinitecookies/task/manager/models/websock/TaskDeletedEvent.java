package infinitecookies.task.manager.models.websock;

public record TaskDeletedEvent(Long boardId, long revision, Long categoryId, Long taskId) {
}
