import type { SerializedEditorState } from "lexical";
import type { CategoryColor } from "./category";
import type { BackgroundType } from "./board";


export const BoardEventType = {
  CATEGORY_CREATED:     { name: "CATEGORY_CREATED"    , group: "CATEGORY" },
  CATEGORY_UPDATED:     { name: "CATEGORY_UPDATED"    , group: "CATEGORY" },
  CATEGORIES_REORDERED: { name: "CATEGORIES_REORDERED", group: "CATEGORY" },
  CATEGORY_DELETED:     { name: "CATEGORY_DELETED"    , group: "CATEGORY" },

  TASK_CREATED:         { name: "TASK_CREATED"   , group: "TASK" },
  TASK_UPDATED:         { name: "TASK_UPDATED"   , group: "TASK" },
  TASKS_REORDERED:      { name: "TASKS_REORDERED", group: "TASK" },
  TASK_DELETED:         { name: "TASK_DELETED"   , group: "TASK" },

  BACKGROUND_UPDATED:   { name: "BACKGROUND_UPDATED", group: "BOARD" }
} as const;

export const boardEventMetadata = Object.fromEntries(
  Object.values(BoardEventType).map(event => [event.name, event])
);

export type BoardEventType =
  typeof BoardEventType[keyof typeof BoardEventType]["name"];

export type BoardEvent = {
  type: BoardEventType;
  payload: any;
  revision: number;
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
  assignedUserIds?: number[];
};

export type TasksReorderedEventPayload = {
  sourceCategoryId?: number;
  targetCategoryId: number;
  sourceTaskIds?: number[];
  targetTaskIds: number[];
  movedTaskId?: number;
};

export type BackgroundUpdatedEventPayload = {
  backgroundType: BackgroundType;
  backgroundSolidColor: string;
  backgroundGradientStartColor: string;
  backgroundGradientEndColor: string;
};
