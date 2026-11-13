package infinitecookies.task.manager.services;

import infinitecookies.task.manager.exceptions.ApiErrorCode;
import infinitecookies.task.manager.exceptions.ApiException;
import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.dto.BoardResponse;
import infinitecookies.task.manager.models.dto.BoardBackgroundUpdateRequest;
import infinitecookies.task.manager.models.websock.BackgroundUpdatedEvent;
import infinitecookies.task.manager.repositories.BoardRepository;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BoardService {

    private final BoardRevisionService boardRevisionService;

    private final BoardRepository repository;

    private final ApplicationEventPublisher applicationEventPublisher;

    public BoardService(
        BoardRevisionService boardRevisionService,
        BoardRepository repository,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.boardRevisionService = boardRevisionService;
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void checkHasAuthorization(Long userId, Long boardId) {
        if (!repository.isUserAllowed(boardId, userId)) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED, ApiErrorCode.BOARD_ACCESS_NOT_AUTHORIZED,
                "Not authorized to access board"
            );
        }
    }

    public List<BoardResponse> getBoards(Long userId) {
        // Note: Retrieving boards is for the purpose of determining which
        //       boards a user has so revision numbers are unimportant here.
        return repository.findByAllowedUsers_Id(userId).stream()
            .map(board -> BoardResponse.fromBoard(board, -1L))
            .toList();
    }

    public BoardResponse getBoard(Long userId, Long boardId) {
        checkHasAuthorization(userId, boardId);

        long revision = boardRevisionService.getRevision(boardId);
        Board board = repository.findById(boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.BOARD_NOT_FOUND, "Board not found"
            ));
        return BoardResponse.fromBoard(board, revision);
    }

    @Transactional
    public void updateBoardBackground(Long userId, Long boardId, @Valid BoardBackgroundUpdateRequest request) {
        checkHasAuthorization(userId, boardId);

        Board board = repository.findById(boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.BOARD_NOT_FOUND, "Board not found"
            ));

        board.setBackgroundType(request.backgroundType());
        board.setBackgroundSolidColor(request.backgroundSolidColor());
        board.setBackgroundGradientStartColor(request.backgroundGradientStartColor());
        board.setBackgroundGradientEndColor(request.backgroundGradientEndColor());

        long revision = boardRevisionService.incrementRevision(boardId);
        applicationEventPublisher.publishEvent(new BackgroundUpdatedEvent(
            boardId,
            revision,
            request.backgroundType(),
            request.backgroundSolidColor(),
            request.backgroundGradientStartColor(),
            request.backgroundGradientEndColor()
        ));
    }
}
