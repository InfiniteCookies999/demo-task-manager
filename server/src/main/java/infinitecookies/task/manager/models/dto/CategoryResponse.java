package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.CategoryColor;

public record CategoryResponse(long id, String name, CategoryColor color, int position) {

    public static CategoryResponse fromCategory(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getColor(),
            category.getPosition()
        );
    }
}
