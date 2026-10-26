package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.InviteResponse;
import infinitecookies.task.manager.services.InviteService;
import infinitecookies.task.manager.session.SessionUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invites")
public class InvitesController {

    private final InviteService service;

    public InvitesController(InviteService service) {
        this.service = service;
    }

    @GetMapping
    public List<InviteResponse> getInvites(SessionUser sessionUser) {
        return service.getInvites(sessionUser.id());
    }

    @PostMapping("/accept/{id}")
    public void acceptInvite(@PathVariable Long id, SessionUser sessionUser) {
        service.acceptInvite(sessionUser.id(), id);
    }

    @DeleteMapping("/{id}")
    public void deleteInvite(@PathVariable Long id, SessionUser sessionUser) {
        service.deleteInvite(sessionUser.id(), id);
    }
}
