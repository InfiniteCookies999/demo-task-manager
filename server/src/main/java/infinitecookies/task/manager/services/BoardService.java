package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Board;
import infinitecookies.task.manager.models.dto.BoardResponse;
import infinitecookies.task.manager.models.dto.BoardBackgroundUpdateRequest;
import infinitecookies.task.manager.models.websock.BackgroundUpdatedEvent;
import infinitecookies.task.manager.repositories.BoardRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BoardService {

    private final BoardRepository repository;

    private final ApplicationEventPublisher applicationEventPublisher;

    public BoardService(
        BoardRepository repository,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void checkHasAuthorization(Long userId, Long boardId) {
        if (!repository.isUserAllowed(boardId, userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized to access board");
        }
    }

    public List<BoardResponse> getBoards(Long userId) {
        return repository.findByAllowedUsers_Id(userId).stream()
            .map(BoardResponse::fromBoard)
            .toList();
    }

    public BoardResponse getBoard(Long userId, Long boardId) {
        checkHasAuthorization(userId, boardId);

        Board board = repository.findById(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found"));
        return BoardResponse.fromBoard(board);
    }

    @Transactional
    public void updateBoardBackground(Long userId, Long boardId, @Valid BoardBackgroundUpdateRequest request) {
        checkHasAuthorization(userId, boardId);

        Board board = repository.findById(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found"));

        board.setBackgroundType(request.backgroundType());
        board.setBackgroundSolidColor(request.backgroundSolidColor());
        board.setBackgroundGradientStartColor(request.backgroundGradientStartColor());
        board.setBackgroundGradientEndColor(request.backgroundGradientEndColor());

        applicationEventPublisher.publishEvent(new BackgroundUpdatedEvent(
            boardId,
            request.backgroundType(),
            request.backgroundSolidColor(),
            request.backgroundGradientStartColor(),
            request.backgroundGradientEndColor()
        ));
    }
}
