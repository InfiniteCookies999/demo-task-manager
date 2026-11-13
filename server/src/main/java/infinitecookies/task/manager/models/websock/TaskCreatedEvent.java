package infinitecookies.task.manager.models.websock;

import infinitecookies.task.manager.models.dto.TaskResponse;

public record TaskCreatedEvent(Long boardId, long revision, TaskResponse task) {
}
