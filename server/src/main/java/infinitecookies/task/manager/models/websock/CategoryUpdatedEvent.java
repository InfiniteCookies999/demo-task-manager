package infinitecookies.task.manager.models.websock;

import infinitecookies.task.manager.models.CategoryColor;

public record CategoryUpdatedEvent(
    Long boardId,
    Long categoryId,
    String name,
    CategoryColor color
) {}
