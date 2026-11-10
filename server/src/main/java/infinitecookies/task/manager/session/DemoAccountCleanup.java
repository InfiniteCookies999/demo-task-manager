package infinitecookies.task.manager.session;

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
        // User -> owned board -> categories -> tasks

        // Note: if we ever have more than demo users we would only want to
        // delete those users where a "demo_user" flag is set. But since this
        // is a demo application we are simply going to delete all the users
        // to cleanup space.

        // clearing the allowed users here should not delete the invites.
        List<User> users = userRepository.findAll();
        for (User user : users) {
            user.getOwnedBoard().getAllowedUsers().clear();
        }

        // Must explicitly clear invites from board for it to delete the
        // user relationships.
        List<Invite> invites = inviteRepository.findAll();
        for (Invite invite : invites) {
            invite.getBoard().getInvites().remove(invite);
        }

        // Clear assignees relational table.
        List<Task> tasks = taskRepository.findAll();
        for (Task task : tasks) {
            task.getAssignees().clear();
        }

        boardRepository.flush();

        // Must call deleteAll() over deleteAllInBatch() because cascade removal
        // needs to occur.
        userRepository.deleteAll();

    }
}
