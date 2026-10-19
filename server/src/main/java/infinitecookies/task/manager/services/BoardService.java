package infinitecookies.task.manager.services;

import infinitecookies.task.manager.repositories.BoardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BoardService {

    private final BoardRepository repository;

    public BoardService(BoardRepository repository) {
        this.repository = repository;
    }

    public void checkHasAuthorization(Long userId, Long boardId) {
        if (!repository.isUserAllowed(boardId, userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized to access board");
        }
    }
}
