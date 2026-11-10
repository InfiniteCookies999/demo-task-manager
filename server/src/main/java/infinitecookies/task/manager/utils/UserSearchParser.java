package infinitecookies.task.manager.utils;

public class UserSearchParser {

    /**
     * @param search The searched for query that potentially has #ID appended
     *               to a username.
     *               For example: "guest#44"
     * @return The search query split into the username and user ID.
     */
    public static UserSearchParts parse(String search) {
        String[] parts = search.split("#", 2);

        String username = parts[0].trim();
        Long userId = null;

        if (parts.length > 1 && !parts[1].isBlank()) {
            try {
                userId = Long.parseLong(parts[1].trim());
            } catch (NumberFormatException ignored) {
                // Ignore invalid ID and continue searching by username.
            }
        }

        return new UserSearchParts(username, userId);
    }
}
