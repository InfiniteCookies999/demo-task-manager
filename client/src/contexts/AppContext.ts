import { createContext, useContext } from "react";
import type { ApiErrorCode } from "../api/ApiError";

export type HandleErrorOptions = {
  ignore?: ApiErrorCode[];
};

type AppContextType = {
  handleError: (err: unknown, options?: HandleErrorOptions) => void;
};

export const AppContext = createContext<AppContextType | null>(null);

export function useAppContext(): AppContextType {
  const context = useContext(AppContext);
  if (context === null) {
    throw new Error("useAppContext must be used within an AppContext.Provider");
  }

  return context;
}
