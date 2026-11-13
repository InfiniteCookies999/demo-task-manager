import { useMemo, useState } from "react";
import type { Category } from "../../types/category";
import { BoardCategoriesContext } from "../../contexts/BoardCategoriesContext";
import type { Task } from "../../types/task";

type BoardCategoriesProviderProps = {
  resynchronizeBoard: () => void;
  children: React.ReactNode;
};


export function BoardCategoriesProvider(
  {
    resynchronizeBoard,
    children
  }: BoardCategoriesProviderProps
) {

  const [categories, setCategories] = useState<Category[]>([]);

  // Store the tasks by category in a Record so that when tasks are moved between
  // different categories they can be found by source/destination category id.
  const [tasksByCategory, setTasksByCategory] = useState<Record<number, Task[]>>({});

  const value = useMemo(() => ({
    categories,
    setCategories,
    tasksByCategory,
    setTasksByCategory,
    resynchronizeBoard,
  }), [categories, tasksByCategory, resynchronizeBoard]);

  return (
    <BoardCategoriesContext.Provider value={value} >
      {children}
    </BoardCategoriesContext.Provider>
  );
}
