package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.BackgroundType;
import infinitecookies.task.manager.models.Board;
import infinitecookies.task.manager.models.dto.BoardBackgroundUpdateRequest;
import infinitecookies.task.manager.models.websock.BackgroundUpdatedEvent;
import infinitecookies.task.manager.repositories.BoardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BoardServiceTests {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private BoardService boardService;

    @Test
    void updateBoardBackgroundLacksAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        BoardBackgroundUpdateRequest request = mock(BoardBackgroundUpdateRequest.class);
        ResponseStatusException e =
            assertThrows(ResponseStatusException.class, () -> boardService.updateBoardBackground(1L, 1L, request));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(boardRepository, never()).findById(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateBoardBackgroundBoardNotFound() {
        BoardBackgroundUpdateRequest request = mock(BoardBackgroundUpdateRequest.class);

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(boardRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> boardService.updateBoardBackground(1L, 1L, request));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateBoardBackgroundSuccessful() {
        Board board = new Board();

        BoardBackgroundUpdateRequest request = new BoardBackgroundUpdateRequest(
            BackgroundType.SOLID,
            "#FFFFFF",
            "#333333",
            "#000000"
        );

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        boardService.updateBoardBackground(1L, 1L, request);

        assertEquals(BackgroundType.SOLID, board.getBackgroundType());
        assertEquals("#FFFFFF", board.getBackgroundSolidColor());
        assertEquals("#333333", board.getBackgroundGradientStartColor());
        assertEquals("#000000", board.getBackgroundGradientEndColor());

        verify(applicationEventPublisher).publishEvent(any(BackgroundUpdatedEvent.class));
    }

    @Test
    void getBoardNotFound() {
        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(boardRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> boardService.getBoard(1L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}
