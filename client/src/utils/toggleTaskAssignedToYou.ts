import type { Task } from "../types/task";
import { findTaskCategoryId } from "./taskFinding";

export function setTaskAssignedToYou(
  assignedToYou: boolean,
  taskId: number,
  categoryId: number,
  taskByCategory: Record<number, Task[]>
): Record<number, Task[]> {


  let foundCategoryId = findTaskCategoryId(taskByCategory, taskId, categoryId);
  if (!foundCategoryId) {
    return taskByCategory;
  }

  const updatedTasks = taskByCategory[foundCategoryId]
    .map((task) => ({
      ...task,
      assignedToYou: task.id === taskId ? assignedToYou : task.assignedToYou
    }));

  return {
    ...taskByCategory,
    [categoryId]: updatedTasks
  };
}
