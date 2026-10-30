import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "./DialogTitleAndClose";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";
import type { Board } from "../types/board";
import { useNavigate } from "react-router-dom";
import type { User } from "../types/user";

type ViewBoardsDialogProps = {
  user: User;
  onClose: () => void;
};

export default function ViewBoardsDialog({ user, onClose }: ViewBoardsDialogProps) {

  const { handleError } = useAppContext();

  const Navigate = useNavigate();

  const dialogRef = useRef<HTMLDialogElement>(null);

  const [boards, setBoards] = useState<Board[]>([]);

  useEffect(() => {
    dialogRef.current?.showModal();

    async function fetchBoards() {
      try {
        const response = await apiFetch("/api/boards");
        const boards: Board[] = await response.json();
        setBoards(boards);
      } catch (err) {
        handleError(err);
      }
    }

    fetchBoards();
  }, []);

  function close() {
    dialogRef.current?.close();
    onClose();
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      onClose={close}
      className="
        m-auto w-[600px] rounded-xl border border-zinc-300
        bg-white p-0 shadow-xl focus:outline-none
      "
    >
      <div className="p-6">
        <DialogTitleAndClose title={<span>Boards</span>} onClose={close} />
        {boards.map((board) => (
          <div
            key={board.id}
            className="flex flex-col gap-4 mt-2 rounded-lg border border-zinc-200 bg-white shadow-sm p-3"
          >
            <span className="text-xl">
              {board.boardOwnerUsername + "#" + board.boardOwnerId}'s Board
              {board.boardOwnerId === user.id && (
                <span className="ml-2 text-md text-[#999]">
                  (your board)
                </span>
              )}
            </span>
            <button
              type="button"
              className="w-fit text-lg rounded-lg border border-zinc-200 bg-zinc-300 px-4 py-2
                         hover:bg-sky-200 cursor-pointer"
              onClick={() => {
                Navigate(`/boards/${board.id}`);
              }}
            >
              Go to board
            </button>
          </div>
        ))}
      </div>
    </dialog>,
    document.body
  );
}
