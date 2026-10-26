package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.Board;
import infinitecookies.task.manager.models.User;
import infinitecookies.task.manager.models.dto.UserAccountResponse;
import infinitecookies.task.manager.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserAccountResponse getOrCreateAccount(String sessionId) {
        return repository.findBySessionId(sessionId)
            .map(UserAccountResponse::fromUser)
            .orElseGet(() -> createOrRetrieve(sessionId));
    }

    private UserAccountResponse createOrRetrieve(String sessionId) {
        // Deal with duplicate request coming in nearly the same time. It's possible
        // in that case for the unique constraint on the session id to fail because
        // it was created by the other request.
        try {
            return createGuestAccount(sessionId);
        } catch (DataIntegrityViolationException e) {
            return repository.findBySessionId(sessionId)
                .map(UserAccountResponse::fromUser)
                .orElseThrow(() -> e);
        }
    }

    private UserAccountResponse createGuestAccount(String sessionId) {

        User user = new User();
        user.setSessionId(sessionId);
        user.setUsername(""); // Make sure not to violate the non-null constraint.

        // Create a default board so the user can start interacting right away.
        Board ownedBoard = new Board();
        ownedBoard.setOwner(user);
        ownedBoard.setAllowedUsers(Set.of(user));
        user.setOwnedBoard(ownedBoard);

        // Save first to generate the database ID.
        user = repository.save(user);
        user.setUsername("guest#" + user.getId());

        return UserAccountResponse.fromUser(user);
    }
}
