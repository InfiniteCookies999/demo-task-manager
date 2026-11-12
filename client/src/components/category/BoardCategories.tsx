import React, { memo, useEffect, useState } from "react";
import type { Category } from "../../types/category";
import type { Task, TaskDropTarget } from "../../types/task";
import { apiFetch } from "../../api/apiFetch";
import { useAppContext } from "../../contexts/AppContext";
import { subscribeToBoardEvent } from "../../utils/boardEventBus";
import CategoryCard from "./CategoryCard";
import { useBoardCategoryEvents } from "../../hooks/useBoardCategoryEvents";
import { useBoardCategoriesContext } from "../../contexts/BoardCategoriesContext";

type BoardCategoriesProps = {
  boardId: number;
  setCategoriesLoaded: React.Dispatch<React.SetStateAction<boolean>>;
};

function BoardCategories(
  {
    boardId,
    setCategoriesLoaded
  }: BoardCategoriesProps
) {

  const { handleError } = useAppContext();

  const {
    categories,
    setCategories,
    tasksByCategory,
    setTasksByCategory,
  } = useBoardCategoriesContext();

  const [draggingCategory, setDraggingCategory] = useState<Category | null>(null);
  const [dropTargetCategoryId, setDropTargetCategoryId] = useState<number | null>(null);

  const [draggingTask, setDraggingTask] = useState<Task | null>(null);
  const [taskDropTarget, setTaskDropTarget] = useState<TaskDropTarget>(null);

  const {
    addTask,
    applyBoardEvent
  } = useBoardCategoryEvents(setCategories, setTasksByCategory);

  useEffect(() => {

    async function fetchBoardContent() {
      try {
        const categoriesResponse = await apiFetch(`/api/boards/${boardId}/categories`);
        const categories: Category[] = await categoriesResponse.json();

        const tasksResponse = await apiFetch(`/api/boards/${boardId}/tasks`);
        const tasks: Task[] = await tasksResponse.json();

        const groupedTasks: Record<number, Task[]> = {};
        for (const category of categories) {
          groupedTasks[category.id] = [];
        }

        for (const task of tasks) {
          groupedTasks[task.categoryId].push(task);
        }

        setCategories(categories);
        setTasksByCategory(groupedTasks);

        setCategoriesLoaded(true);

      } catch (err) {
        handleError(err);
      }
    }
    fetchBoardContent();

    return subscribeToBoardEvent((event) => {
      applyBoardEvent(event);
    });
  }, [boardId, setCategories, setTasksByCategory, setCategoriesLoaded, handleError, applyBoardEvent]);

  return (
    <div className="flex flex-1 items-start gap-4 overflow-x-scroll p-8">
      {categories.map((category, index) => (
        <CategoryCard
          key={category.id}
          category={category}
          prevCategoryId={index > 0 ? categories[index - 1].id : null}
          nextCategoryId={index < categories.length - 1 ? categories[index + 1].id : null}
          tasks={tasksByCategory[category.id] ?? []}
          addTask={addTask}
          draggingCategory={draggingCategory}
          setDraggingCategory={setDraggingCategory}
          dropTargetCategoryId={dropTargetCategoryId}
          setDropTargetCategoryId={setDropTargetCategoryId}
          draggingTask={draggingTask}
          setDraggingTask={setDraggingTask}
          taskDropTarget={taskDropTarget}
          setTaskDropTarget={setTaskDropTarget}
        />
      ))}
    </div>
  );
}

export default memo(BoardCategories);
