package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.User;
import infinitecookies.task.manager.models.dto.SendInviteRequest;
import infinitecookies.task.manager.models.dto.UserInviteSearchResponse;
import infinitecookies.task.manager.services.InviteService;
import infinitecookies.task.manager.session.SessionUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/invites")
public class BoardInvitesController {

    private final InviteService service;

    public BoardInvitesController(InviteService service) {
        this.service = service;
    }

    @PostMapping("/send")
    public void sendInvite(@PathVariable Long boardId,
                           SessionUser sessionUser,
                           @RequestBody SendInviteRequest request) {
        service.createSentInvite(sessionUser.id(), boardId, request);
    }

    @GetMapping("/user-search")
    public List<UserInviteSearchResponse> searchUsersForInvite(
        @PathVariable Long boardId,
        SessionUser sessionUser,

        @RequestParam
        @NotBlank
        @Size(min = 2, max = User.MAX_USERNAME_LENGTH)
        String username
    ) {
        return service.searchUsers(sessionUser.id(), boardId, username);
    }
}
