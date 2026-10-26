package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.Invite;

public record InviteResponse(Long id, Long boardId, String invitedByUsername, String boardOwnerUsername) {
    public static InviteResponse fromInvite(Invite invite) {
        return new InviteResponse(
            invite.getId(),
            invite.getBoard().getId(),
            invite.getInvitedBy().getUsername(),
            invite.getBoard().getOwner().getUsername()
        );
    }
}
