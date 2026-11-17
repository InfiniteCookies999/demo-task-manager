CREATE TABLE board_allowed_users (
    board_id BIGINT NOT NULL,
    user_id  BIGINT NOT NULL,

    CONSTRAINT fk_board_allowed_users_board
        FOREIGN KEY (board_id)
        REFERENCES boards(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_board_allowed_users_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT pk_board_allowed_users
        PRIMARY KEY (board_id, user_id)
);
