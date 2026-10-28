import type { SerializedEditorState } from "lexical";
import type { CategoryColor } from "./category";

export type BoardEvent = {
  type: string;
  payload: any;
};

export type UnprocessedRequest = {
  type: string;
  requestId: string;
  backup?: any;
};

export type CategoryUpdatedEventPayload = {
  categoryId: number;
  name?: string;
  color?: CategoryColor;
};

export type TaskUpdatedEventPayload = {
  taskId: number;
  categoryId: number;
  name?: string;
  description?: SerializedEditorState;
  completed?: boolean;
};

export type TasksReorderedEventPayload = {
  sourceCategoryId?: number;
  targetCategoryId: number;
  sourceTaskIds?: number[];
  targetTaskIds: number[];
  movedTaskId?: number;
};
