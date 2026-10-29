import type { Task } from "../types/task";

export function findTaskCategoryId(
  tasksByCategory: Record<number, Task[]>,
  taskId: number,
  preferredCategoryId: number
) {
  if (tasksByCategory[preferredCategoryId]?.some((task) => task.id === taskId)) {
    return preferredCategoryId;
  }

  const entry = Object.entries(tasksByCategory).find(([, tasks]) =>
    tasks.some((task) => task.id === taskId)
  );

  return entry ? Number(entry[0]) : undefined;
}
