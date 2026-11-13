package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.BackgroundType;
import infinitecookies.task.manager.models.entity.Board;

public record BoardResponse(
    Long id,
    long revision,
    String boardOwnerUsername,
    Long boardOwnerId,
    BackgroundType backgroundType,
    String backgroundSolidColor,
    String backgroundGradientStartColor,
    String backgroundGradientEndColor
) {
    public static BoardResponse fromBoard(Board board, long revision) {
        return new BoardResponse(
            board.getId(),
            revision,
            board.getOwner().getUsername(),
            board.getOwner().getId(),
            board.getBackgroundType(),
            board.getBackgroundSolidColor(),
            board.getBackgroundGradientStartColor(), board.getBackgroundGradientEndColor()
        );
    }
}
