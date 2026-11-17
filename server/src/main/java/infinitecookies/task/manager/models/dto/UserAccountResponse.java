package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.entity.User;

public record UserAccountResponse(Long id, String username, Long ownedBoardId) {
    public static UserAccountResponse fromUser(User user, Long ownedBoardId) {
        return new UserAccountResponse(user.getId(), user.getUsername(), ownedBoardId);
    }
}
