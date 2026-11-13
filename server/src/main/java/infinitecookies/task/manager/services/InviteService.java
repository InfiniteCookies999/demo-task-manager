package infinitecookies.task.manager.services;

import infinitecookies.task.manager.exceptions.ApiErrorCode;
import infinitecookies.task.manager.exceptions.ApiException;
import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.Invite;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.models.dto.InviteResponse;
import infinitecookies.task.manager.models.dto.SendInviteRequest;
import infinitecookies.task.manager.models.dto.UserInviteSearchResponse;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.InviteRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import infinitecookies.task.manager.utils.UserSearchParser;
import infinitecookies.task.manager.utils.UserSearchParts;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InviteService {

    private final InviteRepository inviteRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardService boardService;

    public InviteService(InviteRepository inviteRepository,
                         BoardRepository boardRepository,
                         UserRepository userRepository,
                         BoardService boardService) {
        this.inviteRepository = inviteRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.boardService = boardService;
    }

    @Transactional
    public void createSentInvite(Long userId, Long boardId, SendInviteRequest request) {
        boardService.checkHasAuthorization(userId, boardId);

        if (Objects.equals(userId, request.userId())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, ApiErrorCode.INVITE_CANNOT_INVITE_YOURSELF,
                "Cannot invite yourself"
            );
        }

        User invitedUser = userRepository.findById(request.userId())
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.INVITE_USER_NOT_FOUND, "User not found"
            ));

        if (inviteRepository.existsByInvitedUserIdAndBoardId(invitedUser.getId(), boardId)) {
            throw new ApiException(
                HttpStatus.CONFLICT, ApiErrorCode.INVITE_USER_ALREADY_INVITED, "User already invited"
            );
        }

        if (boardRepository.isUserAllowed(boardId, invitedUser.getId())) {
            throw new ApiException(
                HttpStatus.CONFLICT, ApiErrorCode.INVITE_USER_IS_ALREADY_MEMBER,
                "User is already a member of this board"
            );
        }

        Invite invite = new Invite();
        invite.setInvitedUser(invitedUser);
        invite.setInvitedBy(userRepository.getReferenceById(userId));
        invite.setBoard(boardRepository.getReferenceById(boardId));

        inviteRepository.save(invite);
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getInvites(Long userId) {
        return inviteRepository.findAllByInvitedUserId(userId).stream()
            .map(InviteResponse::fromInvite)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<UserInviteSearchResponse> searchUsers(Long sessionUserId, Long boardId, String search) {
        boardService.checkHasAuthorization(sessionUserId, boardId);

        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.BOARD_NOT_FOUND, "Board not found"
            ));

        Set<Long> allowedUserIds = board.getAllowedUsers().stream()
            .map(User::getId)
            .collect(Collectors.toSet());

        Set<Long> invitedUserIds = board.getInvites().stream()
            .map(invite -> invite.getInvitedUser().getId())
            .collect(Collectors.toSet());

        UserSearchParts searchParts = UserSearchParser.parse(search);

        return userRepository.searchUsersForInvite(
                searchParts.username(),
                searchParts.userId(),
                PageRequest.of(0, 10)
            )
            .stream()
            .filter(user -> !user.getId().equals(sessionUserId))
            .map(user -> new UserInviteSearchResponse(
                user.getId(),
                user.getUsername(),
                invitedUserIds.contains(user.getId()),
                allowedUserIds.contains(user.getId())
            ))
            .toList();
    }

    @Transactional
    public void acceptInvite(Long userId, Long id) {

        Invite invite = inviteRepository.findByIdAndInvitedUserId(id, userId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.INVITE_NOT_FOUND, "Invite not found"
            ));

        invite.getBoard().getAllowedUsers().add(invite.getInvitedUser());

        // The invite is no longer needed the user accepted the invite.
        inviteRepository.delete(invite);
    }

    public void deleteInvite(Long userId, Long id) {

        Invite invite = inviteRepository.findByIdAndInvitedUserId(id, userId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, ApiErrorCode.INVITE_NOT_FOUND, "Invite not found"
            ));

        inviteRepository.delete(invite);
    }
}
