package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.entity.Task;
import infinitecookies.task.manager.models.validation.ValidTaskDescription;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record TaskUpdateRequest(
    @Size(max = Task.MAX_NAME_LENGTH)
    String name,

    @ValidTaskDescription
    JsonNode description,

    Boolean completed,

    @UniqueElements
    List<@NotNull Long> assignedUserIds
) {}
