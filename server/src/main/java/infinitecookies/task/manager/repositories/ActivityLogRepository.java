package infinitecookies.task.manager.repositories;

import infinitecookies.task.manager.models.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}
