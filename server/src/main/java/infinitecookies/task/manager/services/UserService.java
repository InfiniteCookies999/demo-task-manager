package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.models.dto.ChangeUsernameRequest;
import infinitecookies.task.manager.models.dto.UserAccountResponse;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository repository;
    private final BoardRepository boardRepository;

    public UserService(UserRepository repository, BoardRepository boardRepository) {
        this.repository = repository;
        this.boardRepository = boardRepository;
    }

    @Transactional
    public UserAccountResponse getOrCreateAccount(String sessionId) {
        return repository.findBySessionId(sessionId)
            .map(this::getExistingUserAccount)
            .orElseGet(() -> createOrRetrieve(sessionId));
    }

    @Transactional
    public void changeUsername(Long userId, ChangeUsernameRequest request) {
        User user = repository.findById(userId)
            .orElseThrow(); // This is an internal error since it came from SessionUser
        user.setUsername(request.username());
    }

    private UserAccountResponse createOrRetrieve(String sessionId) {
        // Deal with duplicate request coming in nearly the same time. It's possible
        // in that case for the unique constraint on the session id to fail because
        // it was created by the other request.
        try {
            return createGuestAccount(sessionId);
        } catch (DataIntegrityViolationException e) {
            return repository.findBySessionId(sessionId)
                .map(this::getExistingUserAccount)
                .orElseThrow(() -> e);
        }
    }

    private UserAccountResponse getExistingUserAccount(User user) {
        Board board = boardRepository.findByOwnerId(user.getId())
            .orElseThrow(() -> new IllegalStateException("Every user is expected to have a board"));
        return UserAccountResponse.fromUser(user, board.getId());
    }

    private UserAccountResponse createGuestAccount(String sessionId) {

        User user = new User();
        user.setSessionId(sessionId);
        user.setUsername("guest");

        user = repository.save(user);

        // Create a default board so the user can start interacting right away.
        Board ownedBoard = new Board();
        ownedBoard.setOwner(user);
        ownedBoard.setAllowedUsers(Set.of(user));
        boardRepository.save(ownedBoard);

        return UserAccountResponse.fromUser(user, ownedBoard.getId());
    }
}
