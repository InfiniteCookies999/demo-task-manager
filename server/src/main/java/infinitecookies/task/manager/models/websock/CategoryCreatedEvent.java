package infinitecookies.task.manager.models.websock;

import infinitecookies.task.manager.models.dto.CategoryResponse;

public record CategoryCreatedEvent(Long boardId, CategoryResponse category) {
}
