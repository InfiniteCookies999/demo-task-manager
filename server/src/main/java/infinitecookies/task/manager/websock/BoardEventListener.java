package infinitecookies.task.manager.websock;

import infinitecookies.task.manager.models.websock.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for changes to both categories and tasks and when changes get committed
 * this listener relays that information to all the users currently connected
 * to the board via websockets.
 */
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
    public void onCategoryUpdated(CategoryUpdatedEvent event) {
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


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryCreated(TaskCreatedEvent event) {
        send(BoardEventType.TASK_CREATED, event.boardId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryCreated(TaskUpdatedEvent event) {
        send(BoardEventType.TASK_UPDATED, event.boardId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTasksReordered(TasksReorderedEvent event) {
        System.out.println("sending reorder!");
        send(BoardEventType.TASKS_REORDERED, event.boardId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryCreated(TaskDeletedEvent event) {
        send(BoardEventType.TASK_DELETED, event.boardId(), event);
    }

    private void send(BoardEventType type, Long boardId, Object payload) {
        messagingTemplate.convertAndSend("/topic/board/" + boardId, new BoardWebSocketEvent(type, payload));
    }
}
