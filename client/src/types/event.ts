import type { SerializedEditorState } from "lexical";
import type { CategoryColor } from "./category";
import type { BackgroundType } from "./board";

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

export type BackgroundUpdatedEventPayload = {
  backgroundType: BackgroundType;
  backgroundSolidColor: string;
  backgroundGradientStartColor: string;
  backgroundGradientEndColor: string;
};
