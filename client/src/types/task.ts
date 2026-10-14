import type { SerializedEditorState } from "lexical";

export type Task = {
  id: number,
  name: string,
  description: SerializedEditorState,
  completed: boolean
};
