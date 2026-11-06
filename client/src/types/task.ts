import type { SerializedEditorState } from "lexical";

export const MAX_TASKS = 50;

export type Task = {
  id: number;
  categoryId: number;
  name: string;
  description: SerializedEditorState | null;
  completed: boolean;
  position: number;
  createdById: number;
  createdByUsername: string;
  assignedToYou: boolean;
};

export type TaskAssignee = {
  id: number;
  username: string;
  assigned?: boolean;
};

export type TaskAssigneeSearchResult = {
  id: number;
  username: string;
  alreadyAssigned: boolean;
};

export type TaskDropTarget =
  | { type: "task", id: number, categoryId: number }
  | { type: "category", id: number }
  | null;
