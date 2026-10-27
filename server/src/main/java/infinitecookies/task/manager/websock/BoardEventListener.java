package infinitecookies.task.manager.websock;

import infinitecookies.task.manager.models.websock.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BoardEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public BoardEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryCreated(CategoryCreatedEvent event) {
        send(BoardEventType.CATEGORY_CREATED, event.boardId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryUpdated(CategoryUpdateEvent event) {
        send(BoardEventType.CATEGORY_UPDATED, event.boardId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoriesReordered(CategoriesReorderedEvent event) {
        send(BoardEventType.CATEGORIES_REORDERED, event.boardId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryDeleted(CategoryDeletedEvent event) {
        send(BoardEventType.CATEGORY_DELETED, event.boardId(), event);
    }

    private void send(BoardEventType type, Long boardId, Object payload) {
        messagingTemplate.convertAndSend("/topic/board/" + boardId, new BoardWebSocketEvent(type, payload));
    }
}
