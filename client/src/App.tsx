import { useEffect, useRef, useState } from "react";
import { apiFetch } from "./api/apiFetch";
import useApiError from "./hooks/apiError";
import { AppContext } from "./api/AppContext";
import type { User } from "./types/user";
import Board from "./components/Board";


function App() {

  const [ user, setUser ] = useState<User | null>(null);
  const { error, handleError } = useApiError();

  const hasLoggedIn = useRef(false);

  useEffect(() => {
    // Strict Mode will send the session request twice. Preventing that
    // or sessions will be out of sync.
    if (hasLoggedIn.current) return;
    hasLoggedIn.current = true;

    async function fetchUser() {
      try {
        const userResponse = await apiFetch("/api/session", {
          method: "POST"
        });
        const user: User = await userResponse.json();
        setUser(user);
      } catch (err) {
        handleError(err);
      }
    }
    fetchUser();
  }, []);

  return (
    <AppContext.Provider value={{handleError}}>
      {user ? <Board user={user} boardId={user.ownedBoardId} error={error} /> : null}
    </AppContext.Provider>
  );
}

export default App;
