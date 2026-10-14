package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.Task;
import infinitecookies.task.manager.models.validation.ValidTaskDescription;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record TaskUpdateRequest(
    @Size(max = Task.MAX_NAME_LENGTH)
    String name,

    @ValidTaskDescription
    JsonNode description,

    Boolean completed
) {}
