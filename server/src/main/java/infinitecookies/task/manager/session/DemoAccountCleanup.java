package infinitecookies.task.manager.session;

import infinitecookies.task.manager.models.User;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoAccountCleanup implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    public DemoAccountCleanup(UserRepository userRepository,
                              BoardRepository boardRepository) {
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // Cascade:
        // User -> default board -> categories -> tasks

        // Note: if we ever have more than demo users we would only want to
        // delete those users where a "demo_user" flag is set. But since this
        // is a demo application we are simply going to delete all the users
        // to cleanup space.

        List<User> users = userRepository.findAll();
        for (User user : users) {
            user.getOwnedBoard().getAllowedUsers().clear();
        }
        boardRepository.flush();

        // Must call deleteAll() over deleteAllInBatch() because cascade removal
        // needs to occure.
        userRepository.deleteAll();

    }
}
