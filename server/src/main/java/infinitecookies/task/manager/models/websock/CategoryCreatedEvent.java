package infinitecookies.task.manager.models.websock;

import infinitecookies.task.manager.models.dto.CategoryResponse;

public record CategoryCreatedEvent(Long boardId, long revision, CategoryResponse category) {
}
