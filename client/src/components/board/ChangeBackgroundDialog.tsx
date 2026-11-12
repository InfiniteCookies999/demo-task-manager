import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "../shared/DialogTitleAndClose";
import { useAppContext } from "../../contexts/AppContext";
import { type Board } from "../../types/board";
import { apiFetch } from "../../api/apiFetch";

type ChangeBackgroundDialogProps = {
  board: Board;
  setBoard: React.Dispatch<React.SetStateAction<Board | null>>;
  onClose: () => void;
};

type BackgroundSettings = Pick<
  Board,
  | "backgroundType"
  | "backgroundSolidColor"
  | "backgroundGradientStartColor"
  | "backgroundGradientEndColor"
>;


export default function ChangeBackgroundDialog(
  {
    board,
    setBoard,
    onClose
  }: ChangeBackgroundDialogProps) {

  const { handleError } = useAppContext();

  const dialogRef = useRef<HTMLDialogElement>(null);

  const originalBackground = useRef<BackgroundSettings>({
    backgroundType: board.backgroundType,
    backgroundSolidColor: board.backgroundSolidColor,
    backgroundGradientStartColor: board.backgroundGradientStartColor,
    backgroundGradientEndColor: board.backgroundGradientEndColor
  });

  const hasSaved = useRef(false);

  const previewStyle =
    board.backgroundType === "SOLID"
      ? { background: board.backgroundSolidColor }
      : { background: `linear-gradient(135deg,
          ${board.backgroundGradientStartColor},
          ${board.backgroundGradientEndColor})` };

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function close() {
    if (!hasSaved.current) {
      setBoard(current =>
        current
          ? { ...current, ...originalBackground.current }
          : current
      );
    }

    dialogRef.current?.close();
    onClose();
  }

  function updateBoardAppearence(updates: Partial<Board>) {
    setBoard((current) =>
      current ? { ...current, ...updates } : current
    );
  }

  async function saveBackground() {
    try {
      await apiFetch(`/api/boards/${board.id}/background`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
          backgroundType: board.backgroundType,
          backgroundSolidColor: board.backgroundSolidColor,
          backgroundGradientStartColor: board.backgroundGradientStartColor,
          backgroundGradientEndColor: board.backgroundGradientEndColor
        })
      });
      hasSaved.current = true;
      close();
    } catch (err) {
      handleError(err);
    }
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

        <DialogTitleAndClose title={<span>Change Background</span>} onClose={close} />

        {/* Background type selector */}
        <div className="flex gap-2 mb-5">
          <button
            type="button"
            onClick={() => updateBoardAppearence({ backgroundType: "SOLID" })}
            className={`flex-1 rounded-lg border p-3 font-medium cursor-pointer ${
              board.backgroundType === "SOLID"
                ? "border-blue-500 bg-sky-50 text-sky-700"
                : "border-zinc-300 hover:bg-zinc-100"
            }`}
          >
            Solid Color
          </button>

          <button
            type="button"
            onClick={() => updateBoardAppearence({ backgroundType: "GRADIENT" })}
            className={`flex-1 rounded-lg border p-3 font-medium cursor-pointer ${
              board.backgroundType === "GRADIENT"
                ? "border-blue-500 bg-sky-50 text-sky-700"
                : "border-zinc-300 hover:bg-zinc-100"
            }`}
          >
            Gradient
          </button>
        </div>

        {/* Color controls */}
        {board.backgroundType === "SOLID" ? (
          <div className="flex items-center gap-4">
            <label className="font-medium">Background Color</label>
            <input
              type="color"
              value={board.backgroundSolidColor}
              onChange={(e) => {
                updateBoardAppearence({ backgroundSolidColor: e.target.value });
              }}
              className="h-10 w-14 cursor-pointer"
            />
            <span className="text-sm text-zinc-500">{board.backgroundSolidColor}</span>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <label className="font-medium">Start Color</label>
              <input
                type="color"
                value={board.backgroundGradientStartColor}
                onChange={(e) => {
                  updateBoardAppearence({ backgroundGradientStartColor: e.target.value });
                }}
                className="h-10 w-14 cursor-pointer"
              />
            </div>

            <div className="flex items-center justify-between">
              <label className="font-medium">End Color</label>
              <input
                type="color"
                value={board.backgroundGradientEndColor}
                onChange={(e) => {
                  updateBoardAppearence({ backgroundGradientEndColor: e.target.value });
                }}
                className="h-10 w-14 cursor-pointer"
              />
            </div>
          </div>
        )}

        {/* Preview */}
        <div className="mt-6">
          <p className="mb-2 font-medium">Preview</p>
          <div
            className="h-28 w-full rounded-lg border border-zinc-200"
            style={previewStyle}
          />
        </div>

        <button
          type="button"
          className="mt-5 text-xl w-30 rounded-lg bg-blue-500 px-4 py-2 text-white
                     hover:bg-blue-600 cursor-pointer"
          onClick={saveBackground}
        >
          Save
        </button>

      </div>
    </dialog>,
    document.body
  );
}
