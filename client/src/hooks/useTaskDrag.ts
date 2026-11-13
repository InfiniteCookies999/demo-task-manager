import { useCallback } from "react";
import type { Category } from "../types/category";
import { MAX_TASKS, type Task, type TaskDropTarget } from "../types/task";
import createDragClone from "../utils/createDragClone";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";
import { localReorderTasksInCategory } from "../utils/taskReordering";
import { ApiError, ApiErrorCode } from "../api/ApiError";
import { useBoardCategoriesContext } from "../contexts/BoardCategoriesContext";

type UseTaskDragProps = {
  category: Category;
  task: Task | null;
  tasks: Task[];
  setTasks: React.Dispatch<React.SetStateAction<Task[]>>;
  tasksByCategory: Record<number, Task[]>;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>;
  draggingTask: Task | null;
  setDraggingTask: React.Dispatch<React.SetStateAction<Task | null>>;
  setTaskDropTarget: React.Dispatch<React.SetStateAction<TaskDropTarget>>;
};


export function useTaskDrag(
  {
    category,
    task,
    tasks,
    setTasks,
    tasksByCategory,
    setTasksByCategory,
    draggingTask,
    setDraggingTask,
    setTaskDropTarget,
  }:
  UseTaskDragProps
) {

  const { resynchronizeBoard } = useBoardCategoriesContext();

  const { handleError } = useAppContext();

  const clearDragState = useCallback(() => {
    setDraggingTask(null);
    setTaskDropTarget(null);
  }, [setDraggingTask, setTaskDropTarget]);

  // Handle when a task is clicked on to begin dragging.
  //
  const handleDragStart = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      e.stopPropagation(); // Prevent the parent category from dragging.

      // Prevent dragging when dialogs are open.
      if (document.querySelector("dialog:modal")) {
        e.preventDefault();
        return;
      }

      if (!task) {
        return;
      }

      e.dataTransfer.effectAllowed = "move";
      e.dataTransfer.setData("text/plain", JSON.stringify({
        taskId: task.id,
        categoryId: category.id
      }));

      const elm = e.currentTarget.closest("[data-task-id]") as HTMLElement | null;

      if (elm) {

        // Create a clone of the element so we can modify the drag image.
        const clone = createDragClone(elm);
        document.body.appendChild(clone);

        e.dataTransfer.setDragImage(clone, e.nativeEvent.offsetX, e.nativeEvent.offsetY);

        // Once the browser actually transfers to using the clone we can get rid of it.
        requestAnimationFrame(() => {
          clone.remove();
        });
      }

      setDraggingTask(task);
    },
    [task, category, setDraggingTask]
  );

  // Handles when a task enters another task to visually indicate
  // the new order that would be applied to the tasks if the task
  // was dropped.
  //
  const handleDragEnter = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      // Ignore drag events between children elements of the same card.
      if (e.relatedTarget instanceof Node &&
          e.currentTarget.contains(e.relatedTarget)) {
        return;
      }

      if (!draggingTask) {
        return;
      }
      if (!task) {
        return;
      }

      if (tasks.length >= MAX_TASKS && draggingTask.categoryId !== category.id) {
        return;
      }

      setTaskDropTarget({ type: "task", id: task.id, categoryId: category.id });
    },
    [draggingTask, task, tasks, category, setTaskDropTarget]
  );

  // Handles when a task is dropped into another task.
  //
  async function updateTaskOrder(targetCategoryId: number, tasks: Task[], movedTaskId: number | null) {
    try {
      await apiFetch(`/api/boards/${category.boardId}/tasks/reorder`, {
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
    } catch (err) {
      // Check if the tasks are likely out of sync.
      if (err instanceof ApiError) {
        if (err.code === ApiErrorCode.TASK_TARGET_CATEGORY_NOT_FOUND ||
            err.code === ApiErrorCode.TASK_REORDER_MISSING_TASK ||
            err.code === ApiErrorCode.TASK_NOT_FOUND ||
            err.code === ApiErrorCode.TASK_LIMIT_EXCEEDED ||
            err.code === ApiErrorCode.TASK_NEITHER_MOVED_OR_IN_TARGET_CATEGORY ||
            err.code === ApiErrorCode.TASK_MOVED_TASK_MUST_BE_IN_DIFF_CATEGORY
          ) {
          resynchronizeBoard();
          return;
        }
      }

      handleError(err);
    }
  }

  function handleDropInSameCategory(
    tasks: Task[],
    targetCategoryId: number,
    sourceId: number,
    targetId: number,
    clearDragState: () => void
  ) {
    const sourceIndex = tasks.findIndex((task) => task.id === sourceId);
    const targetIndex = tasks.findIndex((task) => task.id === targetId);

    if (sourceIndex === -1 || targetIndex === -1) {
      clearDragState();
      return;
    }

    const reordered = [...tasks];
    const [moved] = reordered.splice(sourceIndex, 1);
    reordered.splice(targetIndex, 0, moved);

    const updated = reordered.map((task, index) => ({
      ...task,
      position: index,
    }));

    setTasks(updated);
    clearDragState();

    updateTaskOrder(targetCategoryId, updated, null);
  }

  function handleDropInDifferentCategory(
    tasksByCategory: Record<number, Task[]>,
    sourceId: number,
    targetId: number,
    sourceCategoryId: number,
    targetCategoryId: number,
    clearDragState: () => void
  ) {

    const sourceTasks = tasksByCategory[sourceCategoryId];
    const targetTasks = tasksByCategory[targetCategoryId];

    const sourceIndex = sourceTasks.findIndex((task) => task.id === sourceId);
    const targetIndex = targetTasks.findIndex((task) => task.id === targetId);
    if (sourceIndex === -1 || targetIndex === -1) {
      clearDragState();
      return;
    }

    if (targetTasks.length >= MAX_TASKS) {
      clearDragState();
      return;
    }

    const updated = localReorderTasksInCategory(
      tasksByCategory,
      sourceId,
      sourceCategoryId,
      targetCategoryId,
      sourceIndex,
      targetIndex
    );

    // Optimistic moving problems are similar to what is described in handleDropInSameCategory()
    // The same proposed solution may be viable.
    setTasksByCategory(updated);
    clearDragState();

    const updatedTargetTasks = updated[targetCategoryId];
    updateTaskOrder(targetCategoryId, updatedTargetTasks, sourceId);
  }

  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>, targetId: number | null, targetCategoryId: number) => {
      e.preventDefault();

      const data = JSON.parse(e.dataTransfer.getData("text/plain"));

      if (data.taskId === undefined || data.categoryId === undefined) {
        // If a category is dropped on top of a task handleDrop will be called so we need
        // to ensure that the source data exists.
        return;
      }

      const sourceId: number = data.taskId;
      const sourceCategoryId: number = data.categoryId;

      if (targetId === null) {
        // This is handled inside useCategoryTaskDrag.ts.
        return;
      }

      if (sourceId === targetId) {
        clearDragState();
        return;
      }

      if (sourceCategoryId === targetCategoryId) {
        handleDropInSameCategory(tasks, targetCategoryId, sourceId, targetId, clearDragState);
        return;
      }

      handleDropInDifferentCategory(
        tasksByCategory, sourceId, targetId, sourceCategoryId, targetCategoryId, clearDragState
      );
    },
    [tasks, tasksByCategory, clearDragState, handleDropInSameCategory, handleDropInDifferentCategory]
  );

  return {
    clearDragState,
    handleDragStart,
    handleDragEnter,
    handleDrop
  };
}
