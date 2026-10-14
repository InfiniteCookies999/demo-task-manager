package infinitecookies.task.manager.models.dto;

import java.util.List;

public record CategoryReorderRequest(List<Entry> entries) {
    public record Entry(Long id) {}
}
