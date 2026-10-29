import { useState } from "react";


export default function useApiError() {
  const [error, setError] = useState("");

  function handleError(err: unknown) {
    setError(err instanceof Error ? err.message : "An unexpected error occurred.");
  }

  function clearError() {
    setError("");
  }

  return {
    error,
    handleError,
    clearError,
  };
}
