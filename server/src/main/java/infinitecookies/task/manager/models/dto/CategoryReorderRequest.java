package infinitecookies.task.manager.models.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CategoryReorderRequest(
    @NotNull
    List<Long> categoryIds
) {
}
