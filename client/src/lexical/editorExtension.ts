import { defineExtension, EditorStateExtension } from "@lexical/extension";
import { RichTextExtension } from "@lexical/rich-text";
import { ListExtension } from "@lexical/list";

export const editorExtension = defineExtension({
  name: "MyEditor",
  namespace: "MyEditor",
  dependencies: [
    RichTextExtension,
    EditorStateExtension,
    ListExtension
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
    list: {
      ul: "editor-list-ul",
      ol: "editor-list-ol",
      listitem: "editor-listitem",
    },
  }
});
