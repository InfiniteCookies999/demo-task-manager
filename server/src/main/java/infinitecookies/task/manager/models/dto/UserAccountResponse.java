package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.User;

public record UserAccountResponse(Long id, String username, Long ownedBoardId) {
    public static UserAccountResponse fromUser(User user) {
        return new UserAccountResponse(user.getId(), user.getUsername(), user.getOwnedBoard().getId());
    }
}
