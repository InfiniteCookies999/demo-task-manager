import { createContext, useContext } from "react";
import type { User } from "../types/user";

/**
 * It is important not to put this in the AppContext because that contains the
 * error handling which is used in a lot of places. If this was placed inside
 * that it would cause a re-render constantly.
 */
type UserContextType = {
  user: User;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
};

export const UserContext = createContext<UserContextType | null>(null);

export function useUserContext(): UserContextType {
  const context = useContext(UserContext);

  if (context === null) {
    throw new Error("useUserContext must be used within a UserContext.Provider");
  }

  return context;
}
