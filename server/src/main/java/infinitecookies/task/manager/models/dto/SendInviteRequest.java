package infinitecookies.task.manager.models.dto;

import jakarta.validation.constraints.NotEmpty;

public record SendInviteRequest(
    @NotEmpty
    Long userId
) {
}
