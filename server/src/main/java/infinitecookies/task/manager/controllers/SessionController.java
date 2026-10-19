package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.UserAccountResponse;
import infinitecookies.task.manager.services.UserAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final UserAccountService service;

    public SessionController(UserAccountService service) {
        this.service = service;
    }

    @PostMapping
    public UserAccountResponse createUserAccount(HttpSession session) {
        UserAccountResponse response = service.getOrCreateAccount(session.getId());
        session.setAttribute("userId", response.id());
        return response;
    }
}
