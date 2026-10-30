package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.BackgroundType;
import infinitecookies.task.manager.models.Board;

public record BoardResponse(
    Long id,
    String boardOwnerUsername,
    Long boardOwnerId,
    BackgroundType backgroundType,
    String backgroundSolidColor,
    String backgroundGradientStartColor,
    String backgroundGradientEndColor
) {
    public static BoardResponse fromBoard(Board board) {
        return new BoardResponse(
            board.getId(),
            board.getOwner().getUsername(),
            board.getOwner().getId(),
            board.getBackgroundType(),
            board.getBackgroundSolidColor(),
            board.getBackgroundGradientStartColor(), board.getBackgroundGradientEndColor()
        );
    }
}
