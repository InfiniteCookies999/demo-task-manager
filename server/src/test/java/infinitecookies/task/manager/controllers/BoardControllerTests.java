package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.BoardBackgroundUpdateRequest;
import infinitecookies.task.manager.services.BoardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoardsController.class)
public class BoardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;

    @Test
    void updateBoardBackgroundWithInvalidColor() throws Exception {
        String json = """
            {
                "backgroundType": "SOLID",
                "backgroundSolidColor": "invalid",
                "backgroundGradientStartColor": "#000000",
                "backgroundGradientEndColor": "#FFFFFF"
            }
            """;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(patch("/api/boards/1/background")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(boardService);
    }

    @Test
    void updateBoardBackgroundWithInvalidBackgroundType() throws Exception {
        String json = """
            {
                "backgroundType": "pizza",
                "backgroundSolidColor": "#FFFFFF",
                "backgroundGradientStartColor": "#000000",
                "backgroundGradientEndColor": "#FFFFFF"
            }
            """;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(patch("/api/boards/1/background")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(boardService);
    }

    @Test
    void updateBoardBackgroundWithValidRequest() throws Exception {
        String json = """
            {
                "backgroundType": "SOLID",
                "backgroundSolidColor": "#FFFFFF",
                "backgroundGradientStartColor": "#000000",
                "backgroundGradientEndColor": "#FFFFFF"
            }
            """;

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(patch("/api/boards/1/background")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());

        verify(boardService).updateBoardBackground(
            eq(1L),
            eq(1L),
            any(BoardBackgroundUpdateRequest.class)
        );
    }
}
