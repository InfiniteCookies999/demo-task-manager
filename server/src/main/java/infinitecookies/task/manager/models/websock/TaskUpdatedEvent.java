package infinitecookies.task.manager.models.websock;

import tools.jackson.databind.JsonNode;

public record TaskUpdatedEvent(
    Long boardId,
    Long taskId,
    String name,
    JsonNode description,
    Boolean completed
) {}
