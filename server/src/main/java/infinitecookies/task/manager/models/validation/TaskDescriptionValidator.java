package infinitecookies.task.manager.models.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

public class TaskDescriptionValidator implements ConstraintValidator<ValidTaskDescription, JsonNode> {

    private static final int MAX_CHARACTERS = 250;
    private static final int MAX_JSON_BYTES = MAX_CHARACTERS * 1024;

    private final ObjectMapper objectMapper;

    public TaskDescriptionValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isValid(JsonNode description, ConstraintValidatorContext context) {
        // Allow for no description.
        if (description == null) {
            return true;
        }
        try {
            String json = objectMapper.writeValueAsString(description);
            if (json.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
                // TODO
                return false;
            }

            JsonNode root = description.get("root");

            // Make sue it matches lexical editor state.
            if (!description.isObject() ||
                root == null ||
                !root.isObject() ||
                !root.has("type") ||
                root.stringValue("type").equals("root")) {
                // TODO
                return false;
            }

            return true;
        } catch (Exception e) {
            // TODO
            return false;
        }
    }
}
