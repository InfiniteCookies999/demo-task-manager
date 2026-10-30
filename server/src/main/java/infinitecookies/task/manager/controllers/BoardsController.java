package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.BoardResponse;
import infinitecookies.task.manager.models.dto.BoardBackgroundUpdateRequest;
import infinitecookies.task.manager.services.BoardService;
import infinitecookies.task.manager.session.SessionUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardsController {

    private final BoardService boardService;

    public BoardsController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/{id}")
    public BoardResponse getBoard(@PathVariable Long id, SessionUser sessionUser) {
        return boardService.getBoard(sessionUser.id(), id);
    }

    @GetMapping
    public List<BoardResponse> getBoards(SessionUser sessionUser) {
        return boardService.getBoards(sessionUser.id());
    }

    @PatchMapping("/{id}/background")
    public void updateBoardBackground(@PathVariable Long id,
                                      SessionUser sessionUser,
                                      @Valid @RequestBody BoardBackgroundUpdateRequest request) {
        boardService.updateBoardBackground(sessionUser.id(), id, request);
    }
}
