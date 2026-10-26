package infinitecookies.task.manager.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    public static final int MAX_USERNAME_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false, length = MAX_USERNAME_LENGTH)
    private String username;

    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, orphanRemoval = true)
    @JoinColumn(name = "default_board_id", nullable = false)
    private Board ownedBoard;

//    @OneToMany(mappedBy = "invitedBy")
//    private List<Invite> sentInvites;
//
//    @OneToMany(mappedBy = "invitedUser")
//    private List<Invite> receivedInvites;

//    @OneToMany(mappedBy = "invitedUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
//    private List<Invite> invites;

}
