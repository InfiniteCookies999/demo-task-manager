package infinitecookies.task.manager.models.websock;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record TaskUpdatedEvent(
    Long boardId,
    Long taskId,
    String name,
    JsonNode description,
    Boolean completed,
    List<Long> assignedUserIds
) {}
