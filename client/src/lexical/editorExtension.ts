import { defineExtension, EditorStateExtension } from "@lexical/extension";
import { RichTextExtension } from "@lexical/rich-text";

export const editorExtension = defineExtension({
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
