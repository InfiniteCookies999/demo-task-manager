import { useEffect, useRef, useState } from "react";
import { apiFetch } from "./api/apiFetch";
import useApiError from "./hooks/useApiError";
import { AppContext } from "./contexts/AppContext";
import type { User } from "./types/user";
import Board from "./components/Board";
import BoardRoute from "./components/BoardRoute";
import { Route, Routes } from "react-router-dom";

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
        const userResponse = await apiFetch("/api/user/session", {
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
      {user && (
        <Routes>
          <Route
            path="*"
            element={
              <Board user={user} setUser={setUser} boardId={user.ownedBoardId} error={error} />
            }
          />
          <Route
            path="/boards/:boardId"
            element={<BoardRoute user={user} setUser={setUser} error={error} />}
          />
        </Routes>
      )}
    </AppContext.Provider>
  );
}

export default App;
