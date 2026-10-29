package infinitecookies.task.manager.websock;

import infinitecookies.task.manager.services.BoardService;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * This interceptor checks if the user is subscribing to the board topic and validates
 * authentication to the board for the websocket connection.
 */
@Component
public class BoardSubscriptionInterceptor implements ChannelInterceptor {

    private static final String BOARD_TOPIC_PREFIX = "/topic/board/";

    private final BoardService boardService;

    public BoardSubscriptionInterceptor(BoardService boardService) {
        this.boardService = boardService;
    }

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(BOARD_TOPIC_PREFIX)) {
            return message;
        }

        long boardId = parseBoardId(destination);

        Map<String, Object> attributes = accessor.getSessionAttributes();
        Object userIdObject = attributes == null ? null : attributes.get("userId");

        if (!(userIdObject instanceof Long userId)) {
            throw new MessageDeliveryException("User session not initialized");
        }

        boardService.checkHasAuthorization(userId, boardId);

        return message;
    }

    private static long parseBoardId(String destination) {
        String id = destination.substring(BOARD_TOPIC_PREFIX.length());

        try {
            long boardId = Long.parseLong(id);
            if (boardId <= 0) {
                throw new NumberFormatException();
            }
            return boardId;
        } catch (NumberFormatException e) {
            throw new MessageDeliveryException("Invalid board destination");
        }
    }
}
