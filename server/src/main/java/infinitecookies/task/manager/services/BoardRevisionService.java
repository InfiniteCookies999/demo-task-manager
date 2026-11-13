package infinitecookies.task.manager.services;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This service manages an in memory counter of revisions that tracks when changes occur on a
 * board. The revision number may be used by the client to help synchronize websocket events
 * with the HTTP responses for getting boards, categories, and tasks when the page for a specific
 * board is initially loaded.
 */
@Component
public class BoardRevisionService {

    private final ConcurrentHashMap<Long, AtomicLong> revisions = new ConcurrentHashMap<>();

    public long incrementRevision(Long boardId) {
        return revisions
            .computeIfAbsent(boardId, _ -> new AtomicLong(0))
            .incrementAndGet();
    }

    public long getRevision(Long boardId) {
        return revisions
            .computeIfAbsent(boardId, _ -> new AtomicLong(0))
            .get();
    }
}
