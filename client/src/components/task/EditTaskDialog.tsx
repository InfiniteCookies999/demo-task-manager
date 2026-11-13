import { SquarePen } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import type { Task, TaskAssignee } from "../../types/task";
import RichTexEditor from "../editor/RichTextEditor";
import type { SerializedEditorState } from "lexical";
import DeleteCollapsable from "../shared/DeleteCollapsable";
import { useAppContext } from "../../contexts/AppContext";
import { apiFetch } from "../../api/apiFetch";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "../shared/DialogTitleAndClose";
import { removeTaskForDelete } from "../../utils/taskDeleting";
import EditTaskAssignees from "./EditTaskAssignees";
import SaveAndCancelButtons from "../shared/SaveAndCancelButtons";
import { ApiErrorCode } from "../../api/ApiError";

type EditTaskDialogProps = {
  boardId: number;
  task: Task,
  setTasks: React.Dispatch<React.SetStateAction<Task[]>>;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>;
  onClose: () => void;
};

export default function EditTaskDialog(
  {
    boardId,
    task,
    setTasks,
    setTasksByCategory,
    onClose
  }:
  EditTaskDialogProps
) {

  const { handleError } = useAppContext();

  const [editName, setEditName] = useState(task.name);

  const [getDescriptionJSON, setGetDescriptionJSON] =
    useState<(() => SerializedEditorState | null) | null>(null);

  const dialogRef = useRef<HTMLDialogElement>(null);

  const originalAssigneesIdsRef = useRef<number[] | null>(null);
  const [assignees, setAssignees] = useState<TaskAssignee[]>([]);

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

    const nameChanged = editName !== task.name;

    const newAssigneeIds = assignees
      .filter((assignee) => assignee.assigned)
      .map((assignee) => assignee.id);

    // Making sure to have a null check on `originalAssigneesIdsRef` in case the user decided
    // to click save before they were downloaded from the server.
    const originalAssigneeIds = originalAssigneesIdsRef.current;
    const assigneesChanged =
      originalAssigneeIds && (
        newAssigneeIds.length !== originalAssigneeIds.length ||
        !newAssigneeIds.every((id) => originalAssigneeIds.includes(id))
      );

    // Optimistically update. If someone else updates after us the correct order
    // will get sent from the server. The server response will simply be what is
    // always accepted in the end anyway so it should stay in sync.
    setTasks((current) =>
      current.map((otherTask) => otherTask.id == task.id
        ? {...otherTask, name: editName, description} : otherTask)
    );

    const request: {
      name?: string;
      description?: typeof description;
      assignedUserIds?: number[];
    } = {};

    if (nameChanged) {
      request.name = editName;
    }
    // Getting lexical to tell us correctly if state has changed seems rather difficult.
    // editor.registerUpdateListener on the lexical editor will post multiple initial startup
    // events. So for now just sending the description each time.
    request.description = description;
    if (assigneesChanged) {
      request.assignedUserIds = newAssigneeIds;
    }

    try {
      await apiFetch(`/api/boards/${boardId}/tasks/${task.id}`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request)
      });
    } catch (err) {
      // Another user may have deleted the task first in which case we can ignore the error.
      handleError(err, {
        ignore: [ ApiErrorCode.TASK_NOT_FOUND ]
      });
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
      setTasksByCategory((current) => removeTaskForDelete(task.id, task.categoryId, current));
    } catch (err) {
      // Another user may have deleted the task first in which case we can ignore the error.
      handleError(err, {
        ignore: [ ApiErrorCode.TASK_NOT_FOUND ]
      });
    }

    close();
  }

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
        <DialogTitleAndClose
          title={
            <>
              <SquarePen></SquarePen>
              <span>Edit Task</span>
            </>
          }
          onClose={close}
        />

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
          <RichTexEditor
            json={task.description}
            getJSON={handleGetJSON}
          />
        </div>

        {/* Assignees */}
        <EditTaskAssignees
          boardId={boardId}
          task={task}
          originalAssigneesIdsRef={originalAssigneesIdsRef}
          assignees={assignees}
          setAssignees={setAssignees}
        />

        <SaveAndCancelButtons onSave={saveTask} onCancel={close} />

        <DeleteCollapsable
          className="mt-4"
          itemId={task.id}
          originalName={task.name}
          onDelete={(deleteName) => deleteTask(deleteName)}
        />

      </div>
    </dialog>,
    document.body
  );
}