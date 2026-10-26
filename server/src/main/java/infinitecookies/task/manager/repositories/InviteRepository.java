package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    boolean existsByInvitedUserIdAndBoardId(Long invitedUserId, Long boardId);

    List<Invite> findAllByInvitedUserId(Long userId);

    Optional<Invite> findByIdAndInvitedUserId(Long id, Long userId);

}
