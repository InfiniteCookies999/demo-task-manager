package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.User;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequest(
    @NotEmpty
    @Size(min = User.MIN_USERNAME_LENGTH, max = User.MAX_USERNAME_LENGTH)
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username must be alphanumeric")
    String username
) {
}
