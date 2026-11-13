import React, { memo, useEffect, useRef, useState } from "react";
import type { Category, CategoryListResponse } from "../../types/category";
import type { Task, TaskDropTarget, TaskListResponse } from "../../types/task";
import { apiFetch } from "../../api/apiFetch";
import { useAppContext } from "../../contexts/AppContext";
import { subscribeToBoardEvent } from "../../utils/boardEventBus";
import CategoryCard from "./CategoryCard";
import { useBoardCategoryEvents } from "../../hooks/useBoardCategoryEvents";
import { useBoardCategoriesContext } from "../../contexts/BoardCategoriesContext";
import { boardEventMetadata } from "../../types/event";

type BoardCategoriesProps = {
  boardId: number;
  syncKey: number;
  setCategoriesLoaded: React.Dispatch<React.SetStateAction<boolean>>;
};

function BoardCategories(
  {
    boardId,
    syncKey,
    setCategoriesLoaded,
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

  // The revision number that the HTTP response provides when downloading the categories
  // and tasks indicates the most recent version of the state of each.
  //
  // These numbers can then be used to ignore out of date websocket events if the HTTP
  // response provided a newer version of the categories/tasks.
  const categoriesDownloadRevision = useRef(0);
  const tasksDownloadRevision = useRef(0);

  const {
    addTask,
    applyBoardEvent
  } = useBoardCategoryEvents();

  useEffect(() => {

    async function fetchBoardContent() {
      try {
        const categoriesResponse = await apiFetch(`/api/boards/${boardId}/categories`);
        const { categories, revision: categoryRevision }: CategoryListResponse = await categoriesResponse.json();

        const tasksResponse = await apiFetch(`/api/boards/${boardId}/tasks`);
        const { tasks, revision: taskRevision }: TaskListResponse = await tasksResponse.json();

        const groupedTasks: Record<number, Task[]> = {};
        for (const category of categories) {
          groupedTasks[category.id] = [];
        }

        for (const task of tasks) {
          groupedTasks[task.categoryId].push(task);
        }

        setCategories(categories);
        setTasksByCategory(groupedTasks);

        categoriesDownloadRevision.current = categoryRevision;
        tasksDownloadRevision.current = taskRevision;

        setCategoriesLoaded(true);

      } catch (err) {
        handleError(err);
      }
    }
    fetchBoardContent();

    return subscribeToBoardEvent((event) => {
      const metadata = boardEventMetadata[event.type];
      // Making sure not to apply updates that are already present
      // in the current state.
      if (metadata.group === "CATEGORY") {
        if (event.revision <= categoriesDownloadRevision.current) {
          return;
        }
      } else if (metadata.group === "TASK") {
        if (event.revision <= tasksDownloadRevision.current) {
          return;
        }
      }
      applyBoardEvent(event);
    });
  }, [boardId, setCategories, setTasksByCategory, setCategoriesLoaded, handleError, applyBoardEvent, syncKey]);

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
