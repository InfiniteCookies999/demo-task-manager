import { useEffect } from "react";
import { apiFetch } from "../../api/apiFetch";
import { useAppContext } from "../../contexts/AppContext";

type UserSearchProps<T> = {
  inputId: string;
  setFoundUsers: React.Dispatch<T[]>;
  setIsSearching: React.Dispatch<boolean>;
  searchedUsername: string;
  setSearchedUsername: React.Dispatch<string>;
  headerText: string;
  searchEndpoint: (username: string) => string;
};


export default function UserSearch<T>(
  {
    inputId,
    setFoundUsers,
    setIsSearching,
    searchedUsername,
    setSearchedUsername,
    headerText,
    searchEndpoint
  }:
  UserSearchProps<T>
) {

  const { handleError } = useAppContext();

  useEffect(() => {
    const username = searchedUsername.trim();

    if (username.length < 2) {
      setFoundUsers([]);
      setIsSearching(false);
      return;
    }

    // Search with timeout of 300 milliseconds.
    let cancelled = false;
    const timeoutId = window.setTimeout(async () => {
      setIsSearching(true);

      try {
        const response = await apiFetch(searchEndpoint(username));

        const results: T[] = await response.json();

        if (!cancelled) {
          setFoundUsers(results);
        }
      } catch (err) {
        if (!cancelled) {
          handleError(err);
        }
      } finally {
        if (!cancelled) {
          setIsSearching(false);
        }
      }
    }, 300);

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    }
  }, [
    searchedUsername,
    setFoundUsers,
    setIsSearching,
    handleError
  ]);

  return (
    <div className="mb-3">
      <label
        htmlFor={inputId}
        className="mb-2 block text-xl font-medium text-zinc-700">
        {headerText}
      </label>
      <input
        id={inputId}
        value={searchedUsername}
        onChange={(e) => setSearchedUsername(e.target.value)}
        maxLength={20}
        className="
          w-full rounded-lg border border-zinc-300 text-lg
          px-3 py-2 outline-none
          focus:border-blue-300
        "
      />
    </div>
  );
}
