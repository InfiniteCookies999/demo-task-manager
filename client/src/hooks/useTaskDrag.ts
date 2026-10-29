import { useCallback } from "react";
import type { Category } from "../types/category";
import type { Task, TaskDropTarget } from "../types/task";
import createDragClone from "../utils/createDragClone";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";
import { localReorderTasksInCategory } from "../utils/taskReordering";

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

      setTaskDropTarget({ type: "task", id: task.id });
    },
    [draggingTask, task, setTaskDropTarget]
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
      handleError(err);
    }
  }

  function handleDropInSameCategory(targetCategoryId: number, sourceId: number, targetId: number) {
    const sourceIndex = tasks.findIndex((task) => task.id === sourceId);
    const targetIndex = tasks.findIndex((task) => task.id === targetId);

    if (sourceIndex === -1 || targetIndex === -1) {
      return;
    }

    const reordered = [...tasks];
    const [moved] = reordered.splice(sourceIndex, 1);
    reordered.splice(targetIndex, 0, moved);

    const updated = reordered.map((task, index) => ({
      ...task,
      position: index,
    }));

    // Optmistic moving problems:
    // 1. We move a card that another user already moved to a different category.
    //    Now our change is rejected but we have no rollback strategy.
    // 2. Another user moves a different card out to a different category so now we
    //    reorder the incorrect number of cards.
    // 3. Another user adds a different card to this category so now we reorder the
    //    incorrect number of cards.
    //
    // Always rolling back would be problematic we could override newer changes.
    //
    // Possible solution:
    // If the server responds with an error send a request to the server asking it
    // where the card we just tried to move should be.
    //
    setTasks(updated);
    updateTaskOrder(targetCategoryId, updated, null);
  }

  function handleDropInDifferentCategory(
    sourceId: number, targetId: number,
    sourceCategoryId: number, targetCategoryId: number
  ) {

    const sourceTasks = tasksByCategory[sourceCategoryId];
    const targetTasks = tasksByCategory[targetCategoryId];

    const sourceIndex = sourceTasks.findIndex((task) => task.id === sourceId);
    const targetIndex = targetTasks.findIndex((task) => task.id === targetId);
    if (sourceIndex === -1 || targetIndex === -1) {
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

    const updatedTargetTasks = updated[targetCategoryId];
    updateTaskOrder(targetCategoryId, updatedTargetTasks, sourceId);
  }

  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>, targetId: number | null, targetCategoryId: number) => {
      e.preventDefault();

      const data = JSON.parse(e.dataTransfer.getData("text/plain"));

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
        handleDropInSameCategory(targetCategoryId, sourceId, targetId);
        clearDragState();
        return;
      }

      handleDropInDifferentCategory(sourceId, targetId, sourceCategoryId, targetCategoryId);
      clearDragState();
    },
    [clearDragState, handleDropInSameCategory, handleDropInDifferentCategory]
  );

  return {
    clearDragState,
    handleDragStart,
    handleDragEnter,
    handleDrop
  };
}
