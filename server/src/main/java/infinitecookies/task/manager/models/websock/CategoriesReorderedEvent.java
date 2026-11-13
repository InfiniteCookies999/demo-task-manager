package infinitecookies.task.manager.models.websock;

import java.util.List;

public record CategoriesReorderedEvent(Long boardId, long revision, List<Long> categoryIds) {
}
