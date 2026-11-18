package infinitecookies.task.manager.services;

import infinitecookies.task.manager.models.dto.UserAccountResponse;
import infinitecookies.task.manager.models.entity.ActivityLog;
import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.repositories.ActivityLogRepository;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserCreationService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final ActivityLogRepository activityLogRepository;

    public UserCreationService(
        UserRepository userRepository,
        BoardRepository boardRepository,
        ActivityLogRepository activityLogRepository
    ) {
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public UserAccountResponse createGuestAccount(String sessionId) {
        User user = new User();
        user.setSessionId(sessionId);
        user.setUsername("guest");

        user = userRepository.save(user);

        Board board = new Board();
        board.setOwner(user);
        board.setAllowedUsers(Set.of(user));
        boardRepository.save(board);

        ActivityLog log = new ActivityLog(user.getId(), "User Created");
        activityLogRepository.save(log);

        return UserAccountResponse.fromUser(user, board.getId());
    }
}
