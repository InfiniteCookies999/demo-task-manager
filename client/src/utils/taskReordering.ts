import type { Task } from "../types/task";

export function localReorderTasksInCategory(
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

function applyPartialReorder(tasks: Task[], taskIds: number[]) {
  // Performing a partial reorder on the tasks. A partial reorder is quite
  // important since the client may add a new task then the server may reply
  // with a re-order. In this case it still needs to re-order the tasks the
  // server does currently know about to keep things ordered.
  const tasksById = new Map(tasks.map((task) => [task.id, task]));
  const orderedIds = new Set<number>();
  return taskIds
    .filter((taskId) => {
      if (!tasksById.has(taskId) || orderedIds.has(taskId)) {
        return false;
      }
      orderedIds.add(taskId);
      return true;
    })
    .map((taskId) => tasksById.get(taskId)!)
    .concat(tasks.filter((task) => !orderedIds.has(task.id)))
    .map((task, index) => ({
      ...task,
      position: index,
    }));
}

export function reorderTasksInCategoryFromResponse(
  tasksByCategory: Record<number, Task[]>,
  sourceCategoryId: number | undefined,
  targetCategoryId: number,
  sourceTaskIds: number[] | undefined,
  targetTaskIds: number[],
  movedTaskId: number | undefined
) {

  if (!sourceCategoryId || !(sourceCategoryId in tasksByCategory)) {
    // Single category task reorder.

    const tasks = tasksByCategory[targetCategoryId];
    if (!tasks) {
      // TODO (maddie): this should probably still reorder source tasks!
      // Category no longer exists.
      return tasksByCategory;
    }

    // Thoughts: If the client locally moves a task out of this category
    // first such that it no longer exists in this category that should be fine
    // since moving out of a category is ID based and the server should still be
    // able to detect and handle the move.
    const reordered = applyPartialReorder(tasks, targetTaskIds);
    return {
      ...tasksByCategory,
      [targetCategoryId]: reordered
    };
  }

  if (!sourceTaskIds) {
    // This is not actually possible at this point.
    console.error("Source task ids missing from server response");
    return tasksByCategory;
  }

  const targetTasks = tasksByCategory[targetCategoryId];
  const sourceTasks = tasksByCategory[sourceCategoryId];

  const movedTask = sourceTasks.find((task) => task.id === movedTaskId);
  if (movedTask) {
    movedTask.categoryId = targetCategoryId;
  }

  const sourceTasksWithoutMovedTask = movedTask
    ? sourceTasks.filter((task) => task.id !== movedTaskId)
    : sourceTasks;
  const reorderedSourceTasks = applyPartialReorder(sourceTasksWithoutMovedTask, sourceTaskIds);

  // It should not be needed to check that the task is not already in the targetTasks because
  // if it came from sourceTasks the expectation is that they are already independent.
  const targetTasksWithMovedTask = movedTask ? [...targetTasks, movedTask] : targetTasks;
  const reorderedTargetTasks = applyPartialReorder(targetTasksWithMovedTask, targetTaskIds);

  return {
    ...tasksByCategory,
    [targetCategoryId]: reorderedTargetTasks,
    [sourceCategoryId]: reorderedSourceTasks
  };
}
