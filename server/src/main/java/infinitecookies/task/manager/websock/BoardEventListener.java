package infinitecookies.task.manager.websock;

import infinitecookies.task.manager.models.websock.CategoriesReorderedEvent;
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
    public void onCategoriesReordered(CategoriesReorderedEvent event) {
        messagingTemplate.convertAndSend("/topic/board/" + event.boardId(), event);
    }

}
