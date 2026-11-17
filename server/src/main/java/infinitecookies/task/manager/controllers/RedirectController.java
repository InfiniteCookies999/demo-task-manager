package infinitecookies.task.manager.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects requests all go to the same page unless the request
 * is for /api or /ws.
 */
@Controller
public class RedirectController {

    @GetMapping(value = {
        "/{path:^(?!api$|ws$)[^\\.]*}",
        "/**/{path:^(?!api$|ws$)[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
