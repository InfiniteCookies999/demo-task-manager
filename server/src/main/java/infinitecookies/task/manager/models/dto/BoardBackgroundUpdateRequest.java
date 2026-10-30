package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.BackgroundType;
import infinitecookies.task.manager.models.Board;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BoardBackgroundUpdateRequest(
    @NotNull
    BackgroundType backgroundType,

    @NotNull
    @Pattern(regexp = Board.VALID_COLOR_PATTERN)
    String backgroundSolidColor,

    @NotNull
    @Pattern(regexp = Board.VALID_COLOR_PATTERN)
    String backgroundGradientStartColor,

    @NotNull
    @Pattern(regexp = Board.VALID_COLOR_PATTERN)
    String backgroundGradientEndColor
) {}
