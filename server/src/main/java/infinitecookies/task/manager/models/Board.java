package infinitecookies.task.manager.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "boards")
@Getter
@Setter
public class Board {

    public static final int MAX_CATEGORIES = 30;
    public static final String VALID_COLOR_PATTERN = "^#[0-9a-fA-F]{6}$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "ownedBoard")
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany
    @JoinTable(
        // Name of the table containing the relational mapping.
        name = "board_allowed_users",
        // Names of the columns in the table.
        joinColumns = @JoinColumn(name = "board_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> allowedUsers = new HashSet<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Invite> invites;

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Category> categories;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackgroundType backgroundType = BackgroundType.GRADIENT;

    @Column(nullable = false)
    private String backgroundSolidColor = "#EFF6FF";

    @Column(nullable = false)
    private String backgroundGradientStartColor = "#EFF6FF";

    @Column(nullable = false)
    private String backgroundGradientEndColor = "#FDF2F8";

}
