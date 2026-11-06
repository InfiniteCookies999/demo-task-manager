package infinitecookies.task.manager.repositories.projections;

import tools.jackson.databind.JsonNode;

public interface TaskProjection {
    Long getId();
    Long getCategoryId();
    String getName();
    JsonNode getDescription();
    boolean getCompleted();
    Integer getPosition();
    Long getCreatedByUserId();
    String getCreatedByUsername();
    boolean getAssignedToYou();
}
