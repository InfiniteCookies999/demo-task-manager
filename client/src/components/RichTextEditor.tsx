import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import { ContentEditable } from "@lexical/react/LexicalContentEditable";
import { LexicalExtensionComposer } from "@lexical/react/LexicalExtensionComposer";
import { useExtensionDependency } from "@lexical/react/useExtensionComponent";
import { useSignalValue } from "@lexical/react/useExtensionSignalValue";
import { RichTextExtension } from "@lexical/rich-text";
import { EditorStateExtension } from '@lexical/extension';

import {
  defineExtension,
  FORMAT_TEXT_COMMAND,
  $getSelection,
  $isRangeSelection,
  type TextFormatType,
  type EditorState,
  type SerializedEditorState
} from "lexical";
import { useEffect, useRef } from "react";

const editorExtension = defineExtension({
  name: "MyEditor",
  namespace: "MyEditor",
  dependencies: [
    RichTextExtension,
    EditorStateExtension
  ],
  theme: {
    text: {
      bold: 'editor-text-bold',
      code: 'editor-text-code',
      hashtag: 'editor-text-hashtag',
      italic: 'editor-text-italic',
      overflowed: 'editor-text-overflowed',
      strikethrough: 'editor-text-strikethrough',
      underline: 'editor-text-underline',
      underlineStrikethrough: 'editor-text-underlineStrikethrough',
    },
  }
});

function ToolBar() {
  //https://stackblitz.com/github/facebook/lexical/tree/main/examples/react-rich?file=src%2FApp.tsx,src%2FStyleImportExportExtension.ts
  const [editor] = useLexicalComposerContext();

  // TODO (maddie): why do we need editorState if we can just do editor.read() and it also seems to work?
  const editorState = useSignalValue(useExtensionDependency(EditorStateExtension).output);

  const {isBold, isItalic, isUnderline} = editorState.read(
    () => {
      const selection = $getSelection();
      const inRange = $isRangeSelection(selection);
      return {
        isBold: inRange && selection.hasFormat('bold'),
        isItalic:  inRange && selection.hasFormat('italic'),
        isUnderline: inRange && selection.hasFormat('underline'),
      };
    },
    {editor}
  );

  const styleButtons = [
    { type: 'bold'     , icon: <strong>B</strong>, active: isBold      },
    { type: 'italic'   , icon: <em>I</em>        , active: isItalic    },
    { type: 'underline', icon: <u>U</u>          , active: isUnderline },
  ];

  return (
    <div
      className="flex items-center gap-1 border border-zinc-300 bg-zinc-50 px-2 py-2 rounded-t-lg">
        {styleButtons.map((button) =>
          <button
            key={button.type}
            onMouseDown={(e) => e.preventDefault()}
            className={`h-8 w-8 items-center justify-center rounded-md text-zinc-700 hover:bg-zinc-200 text-xl
                        cursor-pointer
              ${button.active ? "bg-sky-200" : "bg-zinc-50"}
            `}
            onClick={() => {
              // focus is required or it won't properly select.
              editor.focus();
              editor.dispatchCommand(FORMAT_TEXT_COMMAND, button.type as TextFormatType);
            }}
            type="button"
          >
            {button.icon}
          </button>
        )}
    </div>
  );
}

// must bridge the connection and retreive the editor state inside the context of the
// LexicalExtensionComposer.
function EditorStateBridge({ editorStateRef }: { editorStateRef: React.RefObject<EditorState | null> }) {
  const editorState = useSignalValue(useExtensionDependency(EditorStateExtension).output);

  useEffect(() => {
    editorStateRef.current = editorState;
  }, [editorState, editorStateRef]);

  return null;
}

function LoadEditorState({ json }: { json: SerializedEditorState | null }) {

  const [editor] = useLexicalComposerContext();
  useEffect(() => {
    if (!json) return;

    try {
      const editorState = editor.parseEditorState(json);
      editor.setEditorState(editorState);
    } catch (error) {
      // TODO (maddie): better error handling.
      console.error("Failed to load saved description:", error);
    }
  }, [editor, json]);

  return null;
}

type RichTexEditorProps = {
  json?: SerializedEditorState | null;
  getJSON?: (getter: () => SerializedEditorState | null) => void;
};

function FocusableContentArea({ placeholder }: { placeholder: string }) {
  const [editor] = useLexicalComposerContext();
  return (
    <div className="relative min-h-[200px] border border-t-0 border-zinc-300 rounded-b-lg bg-white p-3 text-xl"
      onClick={() => editor.focus()}
    >
      <ContentEditable
        className="relative outline-none"
        aria-label="Rich text editor"
        aria-placeholder={placeholder}
        placeholder={
          <div className="absolute top-3 left-3 inline-block overflow-hidden text-[#999] pointer-events-none select-none">{placeholder}</div>
        }
      />
    </div>
  );
}

export default function RichTexEditor(
  { json, getJSON }: RichTexEditorProps
) {

  const editorStateRef = useRef<EditorState | null>(null);

  // Only runs when getJSON is called.
  useEffect(() => {
    if (!getJSON) return;
    getJSON(() => {
      return editorStateRef.current?.toJSON() ?? null;
    });
  }, [getJSON]);

  // TODO (maddie): resize-none not working
  const placeholder = "Describe your task...";
  return (
    <>
      {/* Must specify these in order to be able to do multiple style at the same time. */}
      <style>
        {`
          .editor-text-bold {
            font-weight: bold;
          }

          .editor-text-italic {
            font-style: italic;
          }

          .editor-text-underline {
            text-decoration: underline;
          }
        `}
      </style>
      <LexicalExtensionComposer extension={editorExtension} contentEditable={null}>
        {json ? <LoadEditorState json={json} /> : null}
        {getJSON ? <EditorStateBridge editorStateRef={editorStateRef} /> : null}

        <div className="overflow-hidden rounded-lg focus-within:ring-2 focus-within:ring-blue-500">
          <ToolBar/>
          <FocusableContentArea placeholder={placeholder} />
        </div>
      </LexicalExtensionComposer>
    </>
  );
}
