package infinitecookies.task.manager.models.dto;

import jakarta.validation.constraints.NotNull;

public record SendInviteRequest(
    @NotNull
    Long userId
) {
}
