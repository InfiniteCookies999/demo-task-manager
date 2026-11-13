package infinitecookies.task.manager.services;

import infinitecookies.task.manager.exceptions.ApiErrorCode;
import infinitecookies.task.manager.exceptions.ApiException;
import infinitecookies.task.manager.models.dto.SendInviteRequest;
import infinitecookies.task.manager.models.dto.UserInviteSearchResponse;
import infinitecookies.task.manager.models.entity.Board;
import infinitecookies.task.manager.models.entity.Invite;
import infinitecookies.task.manager.models.entity.User;
import infinitecookies.task.manager.repositories.BoardRepository;
import infinitecookies.task.manager.repositories.InviteRepository;
import infinitecookies.task.manager.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InviteServiceTests {

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BoardRevisionService boardRevisionService;

    private InviteService inviteService;

    @BeforeEach
    void setUp() {
        // Manually setup mocks to test authorization at the boardService level.
        BoardService boardService = new BoardService(
            boardRevisionService,
            boardRepository,
            applicationEventPublisher
        );

        inviteService = new InviteService(
            inviteRepository,
            boardRepository,
            userRepository,
            boardService
        );
    }

    @Test
    void createSentInviteLacksBoardAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        SendInviteRequest request = mock(SendInviteRequest.class);
        ApiException e = assertThrows(ApiException.class, () -> inviteService.createSentInvite(1L, 1L, request));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createSentInviteCannotInviteSelf() {

        Long userId = 2L;

        when(boardRepository.isUserAllowed(1L, userId)).thenReturn(true);

        SendInviteRequest request = new SendInviteRequest(userId);
        ApiException e =
            assertThrows(ApiException.class, () -> inviteService.createSentInvite(userId, 1L, request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createSentInviteUserNotFound() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        SendInviteRequest request = new SendInviteRequest(2L);
        ApiException e = assertThrows(ApiException.class, () -> inviteService.createSentInvite(1L, 1L, request));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createSentInviteUserAlreadyInvited() {

        Long invitedUserId = 2L;

        User invitedUser = new User();
        invitedUser.setId(2L);

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(inviteRepository.existsByInvitedUserIdAndBoardId(invitedUserId, 1L)).thenReturn(true);

        SendInviteRequest request = new SendInviteRequest(invitedUserId);
        ApiException e =
            assertThrows(ApiException.class, () -> inviteService.createSentInvite(1L, 1L, request));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals(ApiErrorCode.INVITE_USER_ALREADY_INVITED, e.getCode());
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createSentInviteUserAlreadyAMember() {

        Long invitedUserId = 2L;

        User invitedUser = new User();
        invitedUser.setId(2L);

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(inviteRepository.existsByInvitedUserIdAndBoardId(invitedUserId, 1L)).thenReturn(false);
        when(boardRepository.isUserAllowed(1L, invitedUserId)).thenReturn(true);

        SendInviteRequest request = new SendInviteRequest(invitedUserId);
        ApiException e =
            assertThrows(ApiException.class, () -> inviteService.createSentInvite(1L, 1L, request));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals(ApiErrorCode.INVITE_USER_IS_ALREADY_MEMBER, e.getCode());
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createSentInviteUserSuccessful() {

        Long boardId = 1L;
        Long userId = 1L;
        Long invitedUserId = 2L;

        User inviter = new User();
        inviter.setId(userId);

        User invitedUser = new User();
        invitedUser.setId(2L);

        Board board = new Board();
        board.setId(boardId);


        when(boardRepository.isUserAllowed(boardId, userId)).thenReturn(true);
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));
        when(inviteRepository.existsByInvitedUserIdAndBoardId(invitedUserId, boardId)).thenReturn(false);
        when(boardRepository.isUserAllowed(boardId, invitedUserId)).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(inviter);
        when(boardRepository.getReferenceById(boardId)).thenReturn(board);

        SendInviteRequest request = new SendInviteRequest(invitedUserId);
        inviteService.createSentInvite(userId, boardId, request);

        ArgumentCaptor<Invite> inviteCaptor = ArgumentCaptor.forClass(Invite.class);
        verify(inviteRepository).save(inviteCaptor.capture());

        Invite savedInvite = inviteCaptor.getValue();
        assertEquals(invitedUser.getId(), savedInvite.getInvitedUser().getId());
        assertEquals(userId, savedInvite.getInvitedBy().getId());
        assertEquals(boardId, savedInvite.getBoard().getId());
    }

    @Test
    void searchUsersLacksBoardAuthorization() {

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(false);

        ApiException e = assertThrows(ApiException.class, () -> inviteService.searchUsers(1L, 1L, "guest"));

        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        verify(boardRepository, never()).findById(any());
    }

    @Test
    void searchUsersReturnsMatchingUsers() {

        Long sessionUserId = 1L;
        Long boardId = 1L;

        User sessionUser = new User();
        sessionUser.setId(sessionUserId);
        sessionUser.setUsername("currentUser");

        User invitedUser = new User();
        invitedUser.setId(2L);
        invitedUser.setUsername("invitedUser");

        User allowedUser = new User();
        allowedUser.setId(3L);
        allowedUser.setUsername("allowedUser");

        User normalUser = new User();
        normalUser.setId(4L);
        normalUser.setUsername("normalUser");

        Invite invite = new Invite();
        invite.setInvitedUser(invitedUser);

        Board board = new Board();
        board.setId(boardId);
        board.setAllowedUsers(Set.of(allowedUser));
        board.setInvites(List.of(invite));

        when(boardRepository.isUserAllowed(1L, 1L)).thenReturn(true);
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));
        when(userRepository.searchUsersForInvite("user", null, PageRequest.of(0, 10)))
            .thenReturn(List.of(sessionUser, invitedUser, allowedUser, normalUser));

        List<UserInviteSearchResponse> response = inviteService.searchUsers(sessionUserId, boardId, "user");


        assertEquals(3, response.size());

        assertEquals(2L, response.getFirst().id());
        assertEquals("invitedUser", response.getFirst().username());
        assertTrue(response.get(0).alreadyInvited());
        assertFalse(response.get(0).alreadyMember());

        assertEquals(3L, response.get(1).id());
        assertEquals("allowedUser", response.get(1).username());
        assertFalse(response.get(1).alreadyInvited());
        assertTrue(response.get(1).alreadyMember());

        assertEquals(4L, response.get(2).id());
        assertEquals("normalUser", response.get(2).username());
        assertFalse(response.get(2).alreadyInvited());
        assertFalse(response.get(2).alreadyMember());

        verify(boardRepository).findById(boardId);
        verify(userRepository).searchUsersForInvite("user", null, PageRequest.of(0, 10));
    }

    @Test
    void acceptInviteNotFound() {

        when(inviteRepository.findByIdAndInvitedUserId(1L, 1L)).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> inviteService.acceptInvite(1L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(inviteRepository, never()).delete(any());
    }

    @Test
    void acceptInviteSuccessful() {

        Board board = new Board();

        User invitedUser = new User();
        invitedUser.setId(3L);

        Invite invite = new Invite();
        invite.setBoard(board);
        invite.setInvitedUser(invitedUser);

        when(inviteRepository.findByIdAndInvitedUserId(1L, 1L)).thenReturn(Optional.of(invite));

        inviteService.acceptInvite(1L, 1L);

        assertEquals(1, board.getAllowedUsers().size());
        assertTrue(board.getAllowedUsers().contains(invitedUser));
        verify(inviteRepository).delete(invite);
    }

    @Test
    void deleteInviteNotFound() {

        when(inviteRepository.findByIdAndInvitedUserId(1L, 1L)).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class, () -> inviteService.deleteInvite(1L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        verify(inviteRepository, never()).delete(any());
    }

    @Test
    void deleteInviteSuccessful() {

        Invite invite = new Invite();

        when(inviteRepository.findByIdAndInvitedUserId(1L, 1L)).thenReturn(Optional.of(invite));

        inviteService.deleteInvite(1L, 1L);

        verify(inviteRepository).delete(invite);
    }
}
