import type { SerializedEditorState, SerializedLexicalNode } from "lexical";

export function hasDescription(description: SerializedEditorState | null) {
  if (!description?.root?.children) return false;

  // check for lexical empty case involving a single paragraph child which
  // itself has no children.
  const children: SerializedLexicalNode[] = description.root.children;
  return !(
    children.length === 1 &&
    children[0].type === "paragraph" &&
    (
      "children" in children[0] &&
      Array.isArray(children[0].children) &&
      children[0].children.length === 0
    )
  );
}
