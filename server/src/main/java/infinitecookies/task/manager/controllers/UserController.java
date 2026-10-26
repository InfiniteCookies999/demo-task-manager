package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.ChangeUsernameRequest;
import infinitecookies.task.manager.models.dto.UserAccountResponse;
import infinitecookies.task.manager.services.UserService;
import infinitecookies.task.manager.session.SessionUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/session")
    public UserAccountResponse createUserAccount(HttpSession session) {
        UserAccountResponse response = service.getOrCreateAccount(session.getId());
        session.setAttribute("userId", response.id());
        return response;
    }

    @PatchMapping("/change-username")
    public void changeUsername(SessionUser sessionUser, @Valid @RequestBody ChangeUsernameRequest request) {
        service.changeUsername(sessionUser.id(), request);
    }
}
