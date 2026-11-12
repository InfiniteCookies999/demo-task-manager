import type { BoardEvent } from "../types/event";

type Listener = (event: BoardEvent) => void;

const listeners = new Set<Listener>();

export function subscribeToBoardEvent(listener: Listener) {
  listeners.add(listener);

  // Return an unsubscription function.
  return () => {
    listeners.delete(listener);
  };
}

export function publishBoardEvent(event: BoardEvent) {
  listeners.forEach((listener) => listener(event));
}
