package infinitecookies.task.manager.models.dto;

import java.util.List;

public record CategoryListResponse(long revision, List<CategoryResponse> categories) {
}
