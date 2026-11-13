package infinitecookies.task.manager.models.dto;

import java.util.List;

public record TaskListResponse(long revision, List<TaskResponse> tasks) {
}
