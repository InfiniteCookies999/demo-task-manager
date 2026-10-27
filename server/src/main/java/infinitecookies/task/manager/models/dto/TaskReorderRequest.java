package infinitecookies.task.manager.models.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TaskReorderRequest(
    @NotNull
    Long targetCategoryId,

    @NotNull
    List<Long> taskIds,

    // If a card is being moved from one category to
    // another.
    Long movedCardId
) {}
