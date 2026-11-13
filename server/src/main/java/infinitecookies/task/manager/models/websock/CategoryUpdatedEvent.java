package infinitecookies.task.manager.models.websock;

import infinitecookies.task.manager.models.CategoryColor;

public record CategoryUpdatedEvent(
    Long boardId,
    long revision,
    Long categoryId,
    String name,
    CategoryColor color
) {}
