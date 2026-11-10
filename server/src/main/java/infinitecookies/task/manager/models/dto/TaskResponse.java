package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.entity.Task;
import tools.jackson.databind.JsonNode;

public record TaskResponse(
    Long id,
    Long categoryId,
    String name,
    JsonNode description,
    boolean completed,
    int position,
    Long createdById,
    String createdByUsername,
    boolean assignedToYou
) {
    public static TaskResponse fromTask(Task task, boolean assignedToYou) {
        return new TaskResponse(
            task.getId(),
            task.getCategory().getId(),
            task.getName(),
            task.getDescription(),
            task.isCompleted(),
            task.getPosition(),
            task.getCreatedByUser().getId(),
            task.getCreatedByUser().getUsername(),
            assignedToYou
        );
    }
}
