package infinitecookies.task.manager.controllers;

import infinitecookies.task.manager.models.dto.TaskUpdateRequest;
import infinitecookies.task.manager.services.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TasksController.class)
public class TaskControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @ParameterizedTest
    @ValueSource(strings = {
        """
        {"description": "not an object"}
        """,
        """
        {"description": {}}
        """,
        """
        {"description": {"root": "not an object"}}
        """,
        """
        {"description": {"root": {"type": "paragraph", "children": []}}}
        """,
        """
        {"description": {"root": {"type": "root", "children": "not an array"}}}
        """
    })
    void updateTaskWithInvalidDescription(String json) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(patch("/api/boards/1/tasks/1")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void updateTaskWithDescriptionTooLarge() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        String largeDescription = "a".repeat(250 * 1024);
        String json = """
            {
                "description": {
                    "root": {
                        "type": "root",
                        "children": [
                            {
                                "type": "paragraph",
                                "text": "%s"
                            }
                        ]
                    }
                }
            }
            """.formatted(largeDescription);

        mockMvc.perform(patch("/api/boards/1/tasks/1")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    void updateTaskWithNullDescription() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        String json = """
            {
                "description": null
            }
            """;

        mockMvc.perform(patch("/api/boards/1/tasks/1")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());

        verify(taskService).updateTask(
            eq(1L),
            eq(1L),
            eq(1L),
            any(TaskUpdateRequest.class)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        """
        {"name": "Test task"}
        """,
        """
        {"categoryId": 2}
        """,
        """
        {"categoryId": null, "name": "Test task"}
        """,
        """
        {"categoryId": 2, "name": ""}
        """
    })
    void createTaskWithInvalidRequest(String json) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(post("/api/boards/1/tasks")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }
}
