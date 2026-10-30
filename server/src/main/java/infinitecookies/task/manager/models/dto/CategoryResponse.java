package infinitecookies.task.manager.models.dto;

import infinitecookies.task.manager.models.Category;
import infinitecookies.task.manager.models.CategoryColor;

public record CategoryResponse(Long id, Long boardId, String name, CategoryColor color, int position) {

    public static CategoryResponse fromCategory(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getBoard().getId(),
            category.getName(),
            category.getColor(),
            category.getPosition()
        );
    }
}
