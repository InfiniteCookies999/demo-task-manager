import { useCallback } from "react";
import type { Task, TaskDropTarget } from "../types/task";
import type { Category } from "../types/category";
import { localReorderTasksInCategory } from "../utils/taskReordering";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";

type UseCategoryDragProps = {
  boardId: number;
  setDraggingTask: React.Dispatch<React.SetStateAction<Task | null>>;
  setTaskDropTarget: React.Dispatch<React.SetStateAction<TaskDropTarget>>;
  draggingCategory: Category | null;
  tasks: Task[];
  draggingTask: Task | null;
  taskDropTarget: TaskDropTarget;
  tasksByCategory: Record<number, Task[]>;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>;
};


export function useCategoryTaskDrag(
  {
    boardId,
    setDraggingTask,
    setTaskDropTarget,
    draggingCategory,
    tasks,
    draggingTask,
    taskDropTarget,
    tasksByCategory,
    setTasksByCategory
  }:
  UseCategoryDragProps
) {
  
  const { handleError } = useAppContext();

  const clearTaskDragState = useCallback(() => {
    setDraggingTask(null);
    setTaskDropTarget(null);
  }, [setDraggingTask, setTaskDropTarget]);

  // Handles when a task enters an empty category to visually indicate
  // that the task can be dropped into the category.
  //
  const handleTaskDragEnter = useCallback(
    (e: React.DragEvent<HTMLDivElement>, category: Category) => {
      // Don't set the category as the target when entering a task card.
      if (
        e.target instanceof Element &&
        e.target.closest("[data-task-card]")
      ) {
        return;
      }

      if (draggingCategory || !draggingTask) {
        return;
      }

      // Don't update state multiple times.
      if (taskDropTarget?.type === "category" &&
          taskDropTarget.id === category.id) {
        return;
      }

      setTaskDropTarget({ type: "category", id: category.id });
    },
    [draggingCategory, tasks, draggingTask]
  );

  // Handles when a task is dropped into a category instead of a specific task.
  //
  async function updateTaskOrder(
    boardId: number,
    targetCategoryId: number,
    tasks: Task[],
    movedTaskId: number | null
  ) {
    return await apiFetch(`/api/boards/${boardId}/tasks/reorder`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        targetCategoryId,
        taskIds: tasks.map(({ id }) => id),
        movedTaskId
      })
    });
  }

  const handleTaskDrop = useCallback(
    async (e: React.DragEvent<HTMLDivElement>, targetCategoryId: number) => {
      e.preventDefault();

      if (!draggingTask) {
        return;
      }

      if (!taskDropTarget || taskDropTarget.type !== "category") {
        return;
      }

      // This really should not ever happen.
      if (targetCategoryId !== taskDropTarget.id) {
        clearTaskDragState();
        return;
      }

      const data = JSON.parse(e.dataTransfer.getData("text/plain"));

      const sourceId: number = data.taskId;
      const sourceCategoryId: number = data.categoryId;

      if (targetCategoryId === sourceCategoryId) {
        clearTaskDragState();
        return;
      }


      const sourceTasks = tasksByCategory[sourceCategoryId];

      const sourceIndex = sourceTasks.findIndex((task) => task.id === sourceId);
      if (sourceIndex === -1) {
        clearTaskDragState();
        return;
      }

      // Optimistically update
      const targetTasks = tasksByCategory[targetCategoryId];
      const updated = localReorderTasksInCategory(
        tasksByCategory,
        sourceId,
        sourceCategoryId,
        targetCategoryId,
        sourceIndex,
        targetTasks.length
      );
      setTasksByCategory(updated);
      clearTaskDragState();

      const updatedTargetTasks = updated[targetCategoryId];
      try {
        await updateTaskOrder(boardId, targetCategoryId, updatedTargetTasks, sourceId);
      } catch (err) {
        handleError(err);
      }
    },
    [taskDropTarget, draggingTask, tasksByCategory, boardId]
  );

  return {
    handleTaskDragEnter,
    handleTaskDrop
  };
}