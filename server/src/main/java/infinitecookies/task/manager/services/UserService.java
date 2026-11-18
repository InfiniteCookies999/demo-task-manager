package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.entity.ActivityLog;
import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.models.dto.ChangeUsernameRequest;
import infinitecookies.task.manager.models.dto.UserAccountResponse;
import infinitecookies.task.manager.repositories.ActivityLogRepository;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserCreationService userCreationService;

    public UserService(
        UserRepository userRepository,
        BoardRepository boardRepository,
        ActivityLogRepository activityLogRepository,
        UserCreationService userCreationService
    ) {
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
        this.activityLogRepository = activityLogRepository;
        this.userCreationService = userCreationService;
    }

    public UserAccountResponse getOrCreateAccount(String sessionId) {
        return userRepository.findBySessionId(sessionId)
            .map(this::getExistingUserAccount)
            .orElseGet(() -> createOrRetrieve(sessionId));
    }

    @Transactional
    public void changeUsername(Long userId, ChangeUsernameRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(); // This is an internal error since it came from SessionUser
        user.setUsername(request.username());

        ActivityLog activityLog = new ActivityLog(userId, "Username Changed");
        activityLogRepository.save(activityLog);
    }

    private UserAccountResponse createOrRetrieve(String sessionId) {
        // Deal with duplicate request coming in nearly the same time. It's possible
        // in that case for the unique constraint on the session id to fail because
        // it was created by the other request.
        try {
            return userCreationService.createGuestAccount(sessionId);
        } catch (DataIntegrityViolationException e) {
            return userRepository.findBySessionId(sessionId)
                .map(this::getExistingUserAccount)
                .orElseThrow(() -> e);
        }
    }

    private UserAccountResponse getExistingUserAccount(User user) {
        Board board = boardRepository.findByOwnerId(user.getId())
            .orElseThrow(() -> new IllegalStateException("Every user is expected to have a board"));
        return UserAccountResponse.fromUser(user, board.getId());
    }
}
