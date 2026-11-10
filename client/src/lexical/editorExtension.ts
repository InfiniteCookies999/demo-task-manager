import { configExtension, defineExtension, EditorStateExtension } from "@lexical/extension";
import { RichTextExtension } from "@lexical/rich-text";
import { ListExtension } from "@lexical/list";
import { OverflowExtension } from "@lexical/overflow";
import { MaxLengthExtension } from "./MaxLengthExtension";

export const MAX_LENGTH = 250;

export const editorExtension = defineExtension({
  name: "MyEditor",
  namespace: "MyEditor",
  dependencies: [
    RichTextExtension,
    EditorStateExtension,
    ListExtension,
    OverflowExtension,
    configExtension(MaxLengthExtension, {
      disabled: false,
      maxLength: MAX_LENGTH
    })
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
