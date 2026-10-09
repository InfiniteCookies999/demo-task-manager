package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.CategoryColor;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
    @Size(max = Category.MAX_NAME_LENGTH)
    String name,
    CategoryColor color
) {}
