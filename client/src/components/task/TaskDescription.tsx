import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import { ContentEditable } from "@lexical/react/LexicalContentEditable";
import { LexicalExtensionComposer } from "@lexical/react/LexicalExtensionComposer";
import type { SerializedEditorState } from "lexical";
import { useEffect } from "react";
import { editorExtension } from "../../lexical/editorExtension";

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

export default function TaskDescription({ description }: { description: SerializedEditorState }) {
  return (
    <div className="bg-[#f7f7f8] p-2 rounded-md">
      <LexicalExtensionComposer extension={editorExtension}  contentEditable={null}>
        <TaskDescriptionState description={description} />
      </LexicalExtensionComposer>
    </div>
  );
}
