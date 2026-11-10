package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @ParameterizedTest
    @ValueSource(strings = {
        "",                         // Empty
        "a",                        // Too short
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", // Too long
        "invalid username",         // Contains whitespace
        "username!",                // Special character
    })
    void changeUsernameWithInvalidUsername(String username) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        String json = """
            {
                "username": "%s"
            }
            """.formatted(username);

        mockMvc.perform(patch("/api/user/change-username")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void changeUsernameWithMissingUsername() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(patch("/api/user/change-username")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}
