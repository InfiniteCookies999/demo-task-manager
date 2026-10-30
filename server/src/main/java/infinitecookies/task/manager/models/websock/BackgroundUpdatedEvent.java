package infinitecookies.task.manager.models.websock;

import infinitecookies.task.manager.models.BackgroundType;

public record BackgroundUpdatedEvent(
    Long boardId,
    BackgroundType backgroundType,
    String backgroundSolidColor,
    String backgroundGradientStartColor,
    String backgroundGradientEndColor
) {}
