import { createContext, useContext } from "react";

type AppContextType = {
  handleError: (err: unknown) => void;
};

export const AppContext = createContext<AppContextType | null>(null);

export function useAppContext(): AppContextType {
  const context = useContext(AppContext);
  if (context === null) {
    throw new Error("useAppContext must be used within an AppContext.Provider");
  }

  return context;
}
