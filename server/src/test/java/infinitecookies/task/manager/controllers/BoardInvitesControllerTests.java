package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.User;
import infinitecookies.task.manager.models.dto.UserInviteSearchResponse;
import infinitecookies.task.manager.services.InviteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardInvitesController.class)
public class BoardInvitesControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InviteService service;

    @Test
    void searchUsersForInviteWithUsernameTooShort() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(get("/api/boards/1/invites/user-search")
                .session(session)
                .param("username", "a"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void searchUsersForInviteWithUsernameTooLong() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        String username = "a".repeat(User.MAX_USERNAME_LENGTH + 1);

        mockMvc.perform(get("/api/boards/1/invites/user-search")
                .session(session)
                .param("username", username))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void searchUsersForInviteWithValidUsername() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        UserInviteSearchResponse response = new UserInviteSearchResponse(2L, "testUser", false, false);

        when(service.searchUsers(1L, 1L, "test"))
            .thenReturn(List.of(response));

        mockMvc.perform(get("/api/boards/1/invites/user-search")
                .session(session)
                .param("username", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].username").value("testUser"))
            .andExpect(jsonPath("$[0].alreadyInvited").value(false))
            .andExpect(jsonPath("$[0].alreadyMember").value(false));

        verify(service).searchUsers(1L, 1L, "test");
    }
}
