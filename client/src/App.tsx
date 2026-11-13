import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiFetch } from "./api/apiFetch";
import { AppContext, type HandleErrorOptions } from "./contexts/AppContext";
import type { User } from "./types/user";
import Board from "./components/board/Board";
import BoardRoute from "./components/board/BoardRoute";
import { Route, Routes } from "react-router-dom";
import { UserContext } from "./contexts/UserContext";
import ErrorAlert from "./components/shared/ErrorAlert";
import { ApiError } from "./api/ApiError";


type AuthenicatedAppProps = {
  user: User;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
};

function AuthenicatedApp({ user, setUser }: AuthenicatedAppProps) {

  const userValue = useMemo(() => ({ user, setUser }),
    [user, setUser]
  );

  return (
    <UserContext.Provider value={userValue}>
      <Routes>
        <Route
          path="*"
          element={
            <Board boardId={user.ownedBoardId} />
          }
        />
        <Route
          path="/boards/:boardId"
          element={<BoardRoute />}
        />
      </Routes>
    </UserContext.Provider>
  );
}

function App() {

  const [ user, setUser ] = useState<User | null>(null);
  const [error, setError] = useState("");

  const handleError = useCallback((err: unknown, options?: HandleErrorOptions) => {
    if (err instanceof ApiError && options?.ignore?.includes(err.code)) {
      return;
    }
    setError(err instanceof Error ? err.message : "An unexpected error occurred.");
  }, []);

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

  const appValue = useMemo(() => ({ handleError }),
    [handleError]
  );

  return (
    <AppContext.Provider value={appValue}>
      <>
        {user && <AuthenicatedApp user={user} setUser={setUser} />}
        <div className="fixed bottom-6 left-6 z-50">
          <ErrorAlert error={error}/>
        </div>
      </>
    </AppContext.Provider>
  );
}

export default App;
