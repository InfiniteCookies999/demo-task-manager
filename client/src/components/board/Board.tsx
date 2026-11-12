import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch } from "../../api/apiFetch";
import { useAppContext } from "../../contexts/AppContext";
import { useBoardWebSocket } from "../../hooks/useBoardWebSocket";
import { type BoardEvent } from "../../types/event";
import { ApiError } from "../../api/ApiError";
import { type Board } from "../../types/board";
import BoardCategories from "../category/BoardCategories";
import { publishBoardEvent, subscribeToBoardEvent } from "../../utils/boardEventBus";
import { BoardCategoriesProvider } from "./BoardCategoriesProvider";
import CreateCategoryButton from "../category/CreateCategoryButton";
import BoardDialogs from "./BoardDialogs";
import UserNavButton from "../user/UserNavButton";
import { useBoardAppearenceEvents } from "../../hooks/useBoardAppearenceEvents";

type BoardProps = {
  boardId: number;
};

export default function Board({ boardId }: BoardProps) {

  const { handleError } = useAppContext();

  const [board, setBoard] = useState<Board | null>(null);

  const [notAuthorized, setNotAuthorized] = useState(false);

  const [categoriesLoaded, setCategoriesLoaded] = useState(false);

  const bufferedEvents = useRef<BoardEvent[]>([]);
  const isBufferingEvents = useRef(true);

  const { applyBoardEvent } = useBoardAppearenceEvents(setBoard);

  const handleBoardEvent = useCallback((event: BoardEvent) => {
    if (isBufferingEvents.current) {
      bufferedEvents.current.push(event);
      return;
    }
    publishBoardEvent(event);
  }, []);

  const { connected } = useBoardWebSocket(boardId, handleBoardEvent, !notAuthorized);

  useEffect(() => {
    // Ensure websocket connection before fetching main content otherwise events may happen
    // while not connected leading to missed events.
    if (!connected) {
      return;
    }

    bufferedEvents.current = [];
    isBufferingEvents.current = true;

    async function fetchBoard() {

      try {
        const boardResponse = await apiFetch(`/api/boards/${boardId}`);
        const board: Board = await boardResponse.json();

        setBoard(board);

        document.title = `${board.boardOwnerUsername}'s board | task-manager`;

      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          setNotAuthorized(true);
          return;
        }
        handleError(err);
      }
    }

    fetchBoard();

    return subscribeToBoardEvent((event: BoardEvent) => {
      applyBoardEvent(event);
    });
  }, [connected, boardId, setBoard, handleError, applyBoardEvent]);

  // Check for when categories are reloaded and apply all the queued up buffered
  // web socket events once they are loaded in.
  useEffect(() => {
    if (!categoriesLoaded) {
      return;
    }

    const events = [...bufferedEvents.current];
    bufferedEvents.current = [];
    isBufferingEvents.current = false;

    for (const event of events) {
      publishBoardEvent(event);
    }

  }, [categoriesLoaded]);

  // Render components
  //

  if (notAuthorized) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="rounded-lg bg-white p-8 text-center shadow-md">
          <h1 className="mb-2 text-xl font-semibold text-gray-900">
            Access Denied
          </h1>
          <p className="text-gray-600">
            You are not authorized to access this board.
          </p>
        </div>
      </div>
    );
  }

  if (!board) {
    return null;
  }

  return (
    <BoardCategoriesProvider>
      <div
        className="flex flex-col min-h-screen"
        style={{
          background:
            board.backgroundType === "SOLID"
            ? board.backgroundSolidColor
            : `linear-gradient(to bottom right,
              ${board.backgroundGradientStartColor},
              ${board.backgroundGradientEndColor}
            )`
        }}
      >

        <div className="border-b border-zinc-300 bg-slate-50 px-8 py-6">
          <div className="flex w-full justify-between">
            <div className="flex gap-3">
              <>
                <CreateCategoryButton boardId={board.id} />
                <BoardDialogs
                  board={board}
                  setBoard={setBoard}
                />
              </>
            </div>

            <UserNavButton setBoard={setBoard} />

          </div>
        </div>

        <BoardCategories
          boardId={boardId}
          setCategoriesLoaded={setCategoriesLoaded}
        />
      </div>
    </BoardCategoriesProvider>
  );
}
