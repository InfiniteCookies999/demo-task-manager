import { useEffect, useRef, useState } from "react";
import type { Task, TaskAssignee } from "../../types/task";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "../shared/DialogTitleAndClose";
import TaskCompletedCircle from "./TaskCompletedCircle";
import type { SerializedEditorState } from "lexical";
import { ContentEditable } from "@lexical/react/LexicalContentEditable";
import { LexicalExtensionComposer } from "@lexical/react/LexicalExtensionComposer";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import { editorExtension } from "../../lexical/editorExtension";
import { apiFetch } from "../../api/apiFetch";
import { useAppContext } from "../../contexts/AppContext";
import { hasDescription } from "../../utils/hasDescription";
import { useUserContext } from "../../contexts/UserContext";

type ViewTaskDialogProps = {
  boardId: number;
  task: Task;
  onClose: () => void;
};

function TaskDescriptionState({ description }: { description: SerializedEditorState }) {
  const [editor] = useLexicalComposerContext();

  useEffect(() => {
    editor.setEditable(false);
    editor.setEditorState(
      editor.parseEditorState(JSON.stringify(description))
    );
  }, [editor, description]);

  return (
    <ContentEditable
      className="w-full text-xl text-zinc-700 focus:outline-none"
      aria-label="Task description"
    />
  );
}

function TaskDescription({ description }: { description: SerializedEditorState }) {
  return (
    <div className="bg-[#f7f7f8] p-2 rounded-md">
      <LexicalExtensionComposer extension={editorExtension}  contentEditable={null}>
        <TaskDescriptionState description={description} />
      </LexicalExtensionComposer>
    </div>
  );
}

export default function ViewTaskDialog({ boardId, task, onClose }: ViewTaskDialogProps) {

  const { handleError } = useAppContext();
  const { user } = useUserContext();

  const dialogRef = useRef<HTMLDialogElement>(null);

  const [assignees, setAssignees] = useState<TaskAssignee[]>([]);

  // Reset the form whenever the dialog is opened for a task.
  useEffect(() => {
    dialogRef.current?.showModal();

    async function fetchAssignees() {
      try {
        const response = await apiFetch(`/api/boards/${boardId}/tasks/${task.id}/assignees`);
        const assignees: TaskAssignee[] = await response.json();
        setAssignees(assignees);
      } catch (err) {
        handleError(err);
      }
    }

    fetchAssignees();
  }, [task]);

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

        <DialogTitleAndClose title={<span>{task.name}</span>} onClose={close} />

        <div className="flex items-start gap-2 mb-4">
          <TaskCompletedCircle task={task} clickable={false} />
          <span className="text-lg">{task.completed ? "Completed" : "Not Complete"}</span>
        </div>

        <div>
          <label className="block text-2xl font-medium text-zinc-800 mb-2">
            Description
          </label>
          {task.description && hasDescription(task.description) ? (
            <TaskDescription description={task.description} />
          ) : (
            <div className="text-[#777]">None</div>
          )}
        </div>

        <div className="mt-4">
          <label className="block text-2xl font-medium text-zinc-800 mb-2">
            Assignees
          </label>
          {assignees.length > 0
            ? (
              <div className="flex flex-wrap gap-2 mt-2">
                {assignees.map((assignee) => (
                  <label
                    key={assignee.id}
                    className="
                      flex items-center gap-2 rounded-lg border px-3 py-2
                      border-zinc-300
                      select-none"
                  >
                    <span className="text-sm font-medium">
                      {assignee.username} # {assignee.id}
                    </span>

                    {assignee.id === user.id && (
                      <span className="rounded-md bg-emerald-200 text-emerald-900 px-2 py-0.5 text-sm font-medium">
                        You
                      </span>
                    )}
                  </label>
                ))}
              </div>
            )
            : <div className="text-[#777] py-2">No Assignees</div>
          }
        </div>

      </div>
    </dialog>,
    document.body
  );
}
