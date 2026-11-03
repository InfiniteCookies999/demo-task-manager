package infinitecookies.task.manager.models.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * Validates that the Task description matches roughly what is expected out of a serialized lexical
 * state.
 */
public class TaskDescriptionValidator implements ConstraintValidator<ValidTaskDescription, JsonNode> {

    private static final int MAX_JSON_BYTES = 250 * 1024;

    private final ObjectMapper objectMapper;

    public TaskDescriptionValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isValid(JsonNode description, ConstraintValidatorContext context) {
        // Allow for no description.
        if (description == null || description.isNull()) {
            return true;
        }
        try {
            if (!description.isObject()) {
                return false;
            }

            // Prevent it from being too long.
            String jsonString = objectMapper.writeValueAsString(description);
            if (jsonString.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
                return false;
            }

            // Lexical descriptions should begin with a root node as an object.
            JsonNode root = description.get("root");
            if (root == null || !root.isObject()) {
                return false;
            }

            JsonNode rootType = root.get("type");

            // Make sue it matches lexical editor state.
            if (rootType == null || !rootType.isString() ||
                !rootType.asString().equals("root")) {
                return false;
            }

            JsonNode children = root.get("children");
            return children != null && children.isArray();
        } catch (Exception e) {
            return false;
        }
    }
}
