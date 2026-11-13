import { createContext, useContext } from "react";
import type { Category } from "../types/category";
import type { Task } from "../types/task";

type BoardCategoriesContextType = {
  categories: Category[];
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  tasksByCategory: Record<number, Task[]>;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>;
  resynchronizeBoard: () => void;
};

export const BoardCategoriesContext = createContext<BoardCategoriesContextType | null>(null);

export function useBoardCategoriesContext() {
  const context = useContext(BoardCategoriesContext);

  if (!context) {
    throw new Error(
      "useBoardCategories must be used within BoardCategoriesProvider"
    );
  }

  return context;
}
