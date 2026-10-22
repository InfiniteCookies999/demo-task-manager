import { SquarePen, X } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import type { Task } from "../types/task";
import RichTexEditor from "./RichTextEditor";
import type { SerializedEditorState } from "lexical";
import DeleteCollapsable from "./DeleteCollapsable";
import { useAppContext } from "../api/AppContext";
import { apiFetch } from "../api/apiFetch";
import { createPortal } from "react-dom";

type EditTaskDialogProps = {
  boardId: number;
  task: Task,
  setTasks: React.Dispatch<React.SetStateAction<Task[]>>;
  onClose: () => void;
};

export default function EditTaskDialog(
  {
    boardId,
    task,
    setTasks,
    onClose
  }:
  EditTaskDialogProps
) {

  const { handleError } = useAppContext();

  const [editName, setEditName] = useState(task.name);
  const originalDescriptionRef = useRef<string | null>(
    task.description ? JSON.stringify(task.description) : null
  );

  const [getDescriptionJSON, setGetDescriptionJSON] =
    useState<(() => SerializedEditorState | null) | null>(null);

  const dialogRef = useRef<HTMLDialogElement>(null);

  const handleGetJSON = useCallback(
    (getter: () => SerializedEditorState | null) => {
      setGetDescriptionJSON(() => getter);
    },
    []
  );

  // Reset the form whenever the dialog is opened for a task.
  useEffect(() => {
    dialogRef.current?.showModal();
  }, [task]);

  function close() {
    dialogRef.current?.close();
    onClose();
  }

  async function saveTask() {

    if (!getDescriptionJSON) {
      // TODO (maddie): this really should not happen but what if it does?
      return;
    }

    const description = getDescriptionJSON();
    if (description == null) {
      // TODO (maddie): this really should not happen but what if it does?
      return;
    }

    if (editName === task.name &&
        JSON.stringify(description) === originalDescriptionRef.current
    ) {
      close();
      return;
    }

    setTasks((current) =>
      current.map((otherTask) => otherTask.id == task.id
        ? {...otherTask, name: editName, description} : otherTask)
    );

    try {
      await apiFetch(`/api/boards/${boardId}/tasks/${task.id}`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name: editName,
          description: description
        })
      });
    } catch (err) {
      handleError(err);
    }

    close();
  }

  async function deleteTask(deleteName: string) {
    if (deleteName !== editName) {
      return;
    }

    try {
      await apiFetch(`/api/boards/${boardId}/tasks/${task.id}`, {
        method: "DELETE",
      });
      setTasks((current) =>
        current.filter((otherTask) => otherTask.id !== task.id));
    } catch (err) {
      handleError(err);
    }

    close();
  }

  // TODO (maddie): need to put a character limit on the description. because it may be too
  // difficult to enforce in the text box itself it could go as a check when save is pressed.
  return createPortal(
    <dialog
      ref={dialogRef}
      onClose={close}
      className="
        m-auto w-[600px] rounded-xl border border-zinc-300
        bg-white p-0 shadow-xl
      "
    >
      <div className="p-6">
        <button
          type="button"
          onClick={close}
          className="
            absolute right-4 top-4
            rounded-lg p-1.5
            text-zinc-500
            hover:bg-zinc-100 hover:text-zinc-800
            focus:outline-none
            cursor-pointer
          "
          aria-label="Close"
        >
          <X className="h-7 w-7" />
        </button>

        <h2 className="mb-6 text-2xl font-semibold flex items-center gap-3">
          <SquarePen></SquarePen>
          <span>Edit Task</span>
        </h2>

        {/* Name */}
        <div className="mb-3">
          <label
            htmlFor={`task-name-${task.id}`}
            className="mb-2 block text-xl font-medium text-zinc-700">
            Name
          </label>
          <input
            id={`task-name-${task.id}`}
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            maxLength={100}
            className="
              w-full rounded-lg border border-zinc-300 text-lg
              px-3 py-2 outline-none
              focus:border-blue-300
            "
          />
        </div>

        {/* Description */}
        <div className="mb-3">
          <label
            className="block text-xl font-medium text-zinc-700 mb-2">
            Description
          </label>
          <RichTexEditor json={task.description} getJSON={handleGetJSON}></RichTexEditor>
        </div>

        {/* Save/Delete buttons */}
        <button
          type="button"
          className="text-xl w-30 rounded-lg bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 cursor-pointer"
          onClick={saveTask}
        >
          Save
        </button>

        <DeleteCollapsable
          className="mt-8"
          itemId={task.id}
          originalName={task.name}
          onDelete={(deleteName) => deleteTask(deleteName)}
        />

      </div>
    </dialog>,
    document.body
  );
}