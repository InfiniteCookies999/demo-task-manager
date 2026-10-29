package infinitecookies.task.manager.models.websock;

import java.util.List;

public record TasksReorderedEvent(
    Long boardId,
    Long sourceCategoryId,
    Long targetCategoryId,
    List<Long> sourceTaskIds,
    List<Long> targetTaskIds,
    Long movedTaskId
) {}
