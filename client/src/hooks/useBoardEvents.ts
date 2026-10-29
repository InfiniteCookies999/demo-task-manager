import { useCallback } from "react";
import type { Category } from "../types/category";
import type { Task } from "../types/task";
import type { BoardEvent, CategoryUpdatedEventPayload, TasksReorderedEventPayload, TaskUpdatedEventPayload } from "../types/event";
import { reorderCategories } from "../utils/categoryReordering";
import { removeCategoryForDelete } from "../utils/categoryDeleting";
import { findTaskCategoryId } from "../utils/taskFinding";
import { reorderTasksInCategoryFromResponse } from "../utils/taskReordering";
import { removeTaskForDelete } from "../utils/taskDeleting";

export function useBoardEvents(
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>,
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>
) {

  const addCategory = useCallback((category: Category) => {
    setCategories((current) => {
      // Prevent duplicates from WebSocket events.
      if (current.some((other) => other.id === category.id)) {
        return current;
      }

      // Preserve category ordering.
      return [...current, category].sort(
        (a, b) => a.position - b.position
      );
    });

    setTasksByCategory((current) => {
      // Prevent duplicate category initialization.
      if (category.id in current) {
        return current;
      }

      return { ...current, [category.id]: [] };
    });
  }, [setCategories, setTasksByCategory]);

  const addTask = useCallback((task: Task) => {
    setTasksByCategory((current) => {
      // Ignore events for categories that aren't present.
      if (!(task.categoryId in current)) {
        return current;
      }

      // Prevent duplicate tasks from WebSocket events.
      if (
        current[task.categoryId].some(
          (other) => other.id === task.id
        )
      ) {
        return current;
      }

      const updatedTasks = [
        ...current[task.categoryId],
        task,
      ].sort((a, b) => a.position - b.position);

      return {
        ...current,
        [task.categoryId]: updatedTasks,
      };
    });
  }, [setTasksByCategory]);

  const applyBoardEvent = useCallback((event: BoardEvent) => {
    switch (event.type) {
      case "CATEGORY_CREATED": {
        addCategory(event.payload.category);
        break;
      }
      case "CATEGORY_UPDATED": {
        const { categoryId, name, color }: CategoryUpdatedEventPayload = event.payload;

        setCategories((current) =>
          current.map((category) => category.id === categoryId
            ? {
                ...category,
                name: name ?? category.name,
                color: color ?? category.color
              }
            : category
          ));
        break;
      }
      case "CATEGORIES_REORDERED": {
        setCategories((current) => reorderCategories(current, event.payload.categoryIds));
        break;
      }
      case "CATEGORY_DELETED": {
        const categoryId: number = event.payload.categoryId;
        setCategories((current) => removeCategoryForDelete(categoryId, current));
        break;
      }

      case "TASK_CREATED": {
        addTask(event.payload.task);
        break;
      }
      case "TASK_UPDATED": {
        const {
          categoryId,
          taskId,
          name,
          description,
          completed
        }: TaskUpdatedEventPayload = event.payload;

        setTasksByCategory((current) => {

          let foundCategoryId = findTaskCategoryId(current, taskId, categoryId);
          if (!foundCategoryId) {
            return current;
          }

          const tasks = current[foundCategoryId];
          const reordered = tasks
            .map((task) => task.id === taskId
              ? {
                  ...task,
                  name: name ?? task.name,
                  description: description ?? task.description,
                  completed: completed ?? task.completed
                }
              : task
            );

          return {
            ...current,
            [foundCategoryId]: reordered
          };
        });
        break;
      }
      case "TASKS_REORDERED": {
        const {
          sourceCategoryId,
          targetCategoryId,
          sourceTaskIds,
          targetTaskIds,
          movedTaskId
        }: TasksReorderedEventPayload = event.payload;

        setTasksByCategory((current) =>
          reorderTasksInCategoryFromResponse(
            current,
            sourceCategoryId,
            targetCategoryId,
            sourceTaskIds,
            targetTaskIds,
            movedTaskId
        ));

        break;
      }
      case "TASK_DELETED": {
        const taskId: number = event.payload.taskId;
        const categoryId: number = event.payload.categoryId;
        setTasksByCategory((current) => removeTaskForDelete(taskId, categoryId, current));
        break;
      }
    }
  }, [addCategory, addTask, setCategories, setTasksByCategory]);

  return {
    addCategory,
    addTask,
    applyBoardEvent
  };
}
