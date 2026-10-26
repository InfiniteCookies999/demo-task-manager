package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySessionId(String sessionId);

    List<User> findTop10ByUsernameContainingIgnoreCase(String username);

}
