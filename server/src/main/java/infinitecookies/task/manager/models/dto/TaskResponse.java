package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.Task;
import tools.jackson.databind.JsonNode;

public record TaskResponse(
    Long id,
    Long categoryId,
    String name,
    JsonNode description,
    boolean completed,
    int position
) {
    public static TaskResponse fromTask(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getCategory().getId(),
            task.getName(),
            task.getDescription(),
            task.isCompleted(),
            task.getPosition()
        );
    }
}
