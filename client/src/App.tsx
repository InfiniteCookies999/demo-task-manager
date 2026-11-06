import { useEffect, useRef, useState } from "react";
import { apiFetch } from "./api/apiFetch";
import useApiError from "./hooks/useApiError";
import { AppContext } from "./contexts/AppContext";
import type { User } from "./types/user";
import Board from "./components/Board";
import BoardRoute from "./components/BoardRoute";
import { Route, Routes } from "react-router-dom";
import { UserContext } from "./contexts/UserContext";
import ErrorAlert from "./components/ErrorAlert";

function App() {

  const [ user, setUser ] = useState<User | null>(null);
  const { error, handleError } = useApiError();

  const hasLoggedIn = useRef(false);

  useEffect(() => {
      document.title = "task-manager";

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
      <>
        {user && (
          <UserContext.Provider value={{user, setUser}}>
            <Routes>
              <Route
                path="*"
                element={
                  <Board boardId={user.ownedBoardId} error={error} />
                }
              />
              <Route
                path="/boards/:boardId"
                element={<BoardRoute error={error} />}
              />
            </Routes>
          </UserContext.Provider>
        )}
        <div className="fixed bottom-6 left-6 z-50">
          <ErrorAlert error={error}/>
        </div>
      </>
    </AppContext.Provider>
  );
}

export default App;
