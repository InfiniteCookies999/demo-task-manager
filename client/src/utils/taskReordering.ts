import type { Task } from "../types/task";

export function reorderTasksInCategory(
  tasksByCategory: Record<number, Task[]>,
  sourceId: number,
  sourceCategoryId: number,
  targetCategoryId: number,
  sourceIndex: number,
  targetIndex: number
) {
  const sourceTasks = tasksByCategory[sourceCategoryId];
  const targetTasks = tasksByCategory[targetCategoryId];

  const movedTask = {
    ...sourceTasks[sourceIndex],
    categoryId: targetCategoryId
  };

  const updatedSourceTasks = sourceTasks
    .filter((task) => task.id !== sourceId)
    .map((task, index) => ({
      ...task,
      position: index
    }));

  const reorderedTargetTasks = [...targetTasks];
  reorderedTargetTasks.splice(targetIndex + 1, 0, movedTask);

  const updatedTargetTasks = reorderedTargetTasks.map((task, index) => ({
    ...task,
    position: index,
  }));

  return {
    ...tasksByCategory,
    [sourceCategoryId]: updatedSourceTasks,
    [targetCategoryId]: updatedTargetTasks
  };
}
