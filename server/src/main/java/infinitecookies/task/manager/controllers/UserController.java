package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.UserAccountResponse;
import infinitecookies.task.manager.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
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
}
