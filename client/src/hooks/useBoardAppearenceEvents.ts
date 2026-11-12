import { useCallback } from "react";
import type { BackgroundUpdatedEventPayload, BoardEvent } from "../types/event";
import type { Board } from "../types/board";

export function useBoardAppearenceEvents(
  setBoard: React.Dispatch<React.SetStateAction<Board | null>>
) {

  const applyBoardEvent = useCallback((event: BoardEvent) => {
    if (event.type === "BACKGROUND_UPDATED") {
      const {
        backgroundType,
        backgroundSolidColor,
        backgroundGradientStartColor,
        backgroundGradientEndColor
      }: BackgroundUpdatedEventPayload = event.payload;

      setBoard((current) => {
        if (!current) return null;

        return {
          ...current,
          backgroundType: backgroundType,
          backgroundSolidColor: backgroundSolidColor,
          backgroundGradientStartColor: backgroundGradientStartColor,
          backgroundGradientEndColor: backgroundGradientEndColor
        };
      });
    }
  }, [setBoard]);

  return {
    applyBoardEvent
  };
}
