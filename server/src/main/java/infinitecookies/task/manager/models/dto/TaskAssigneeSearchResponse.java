package infinitecookies.task.manager.models.dto;

public record TaskAssigneeSearchResponse(
    Long id,
    String username,
    boolean alreadyAssigned
) {}
