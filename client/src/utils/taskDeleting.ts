import type { Task } from "../types/task";
import { findTaskCategoryId } from "./taskFinding";

export function removeTaskForDelete(
  taskId: number,
  categoryId: number,
  taskByCategory: Record<number, Task[]>
): Record<number, Task[]> {


  let foundCategoryId = findTaskCategoryId(taskByCategory, taskId, categoryId);
  if (!foundCategoryId) {
    return taskByCategory;
  }

  const updatedTasks = taskByCategory[foundCategoryId].filter((task) => task.id !== taskId)
    .map((task, index) => ({
      ...task,
      position: index
    }));

  return {
    ...taskByCategory,
    [categoryId]: updatedTasks
  };
}
