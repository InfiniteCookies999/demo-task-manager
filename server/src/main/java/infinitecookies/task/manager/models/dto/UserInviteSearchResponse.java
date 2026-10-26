package infinitecookies.task.manager.models.dto;

public record UserInviteSearchResponse(
    Long id,
    String username,
    boolean alreadyInvited,
    boolean alreadyMember
) {
}
