package infinitecookies.task.manager.session;

import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.Invite;
import infinitecookies.task.manager.models.entity.Task;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.InviteRepository;
import infinitecookies.task.manager.repositories.TaskRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DemoAccountCleanup implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final InviteRepository inviteRepository;
    private final TaskRepository taskRepository;

    public DemoAccountCleanup(UserRepository userRepository,
                              BoardRepository boardRepository,
                              InviteRepository inviteRepository,
                              TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
        this.inviteRepository = inviteRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {

        // Cascade:
        // Board -> categories -> tasks

        // Note: The boards are deleted first because there is no specified field
        // for the board the user owns in the User entity.

        // Must call deleteAll() over deleteAllInBatch() because cascade removal
        // needs to occur.
        boardRepository.deleteAll();
        userRepository.deleteAll();

    }
}
