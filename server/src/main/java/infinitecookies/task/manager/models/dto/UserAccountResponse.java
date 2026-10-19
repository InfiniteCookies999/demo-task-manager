package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.User;

public record UserAccountResponse(String username, Long id, Long ownedBoardId) {
    public static UserAccountResponse fromUser(User user) {
        return new UserAccountResponse(user.getUsername(), user.getId(), user.getOwnedBoard().getId());
    }
}
