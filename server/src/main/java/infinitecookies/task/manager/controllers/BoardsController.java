package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.BoardResponse;
import infinitecookies.task.manager.services.BoardService;
import infinitecookies.task.manager.session.SessionUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardsController {

    private final BoardService boardService;

    public BoardsController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public List<BoardResponse> getBoards(SessionUser sessionUser) {
        return boardService.getBoards(sessionUser.id());
    }

}
