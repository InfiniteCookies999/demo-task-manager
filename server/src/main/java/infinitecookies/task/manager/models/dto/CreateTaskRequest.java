package infinitecookies.task.manager.models.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateTaskRequest(
    @NotNull
    Long categoryId,

    @NotEmpty
    String name
) {}
