package infinitecookies.task.manager.models.websock;

import infinitecookies.task.manager.models.BackgroundType;

public record BackgroundUpdatedEvent(
    Long boardId,
    long revision,
    BackgroundType backgroundType,
    String backgroundSolidColor,
    String backgroundGradientStartColor,
    String backgroundGradientEndColor
) {}
