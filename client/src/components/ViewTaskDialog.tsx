import { useEffect, useRef } from "react";
import type { Task } from "../types/task";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "./DialogTitleAndClose";
import TaskCompletedCircle from "./TaskCompletedCircle";
import type { SerializedEditorState } from "lexical";
import { ContentEditable } from "@lexical/react/LexicalContentEditable";
import { LexicalExtensionComposer } from "@lexical/react/LexicalExtensionComposer";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import { editorExtension } from "../lexical/editorExtension";

type ViewTaskDialogProps = {
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
    <LexicalExtensionComposer extension={editorExtension}  contentEditable={null}>
      <TaskDescriptionState description={description} />
    </LexicalExtensionComposer>
  );
}

export default function ViewTaskDialog({ task, onClose }: ViewTaskDialogProps) {

  const dialogRef = useRef<HTMLDialogElement>(null);

  // Reset the form whenever the dialog is opened for a task.
    useEffect(() => {
      dialogRef.current?.showModal();
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
          <label
            className="block text-2xl font-medium text-zinc-700 mb-2">
            Description
          </label>
          {task.description ? (
            <TaskDescription description={task.description} />
          ) : (
            <div>None</div>
          )}
        </div>

      </div>
    </dialog>,
    document.body
  );
}
