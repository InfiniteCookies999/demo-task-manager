import { Plus } from "lucide-react";
import { useBoardCategoriesContext as useBoardCategoriesContext } from "../../contexts/BoardCategoriesContext";
import ActionButton from "../board/ActionButton";
import { apiFetch } from "../../api/apiFetch";
import type { Category } from "../../types/category";
import { useAppContext } from "../../contexts/AppContext";
import { useBoardCategoryEvents } from "../../hooks/useBoardCategoryEvents";
import { ApiErrorCode } from "../../api/ApiError";

type CreateCategoryButtonProps = {
  boardId: number;
};

export default function CreateCategoryButton({ boardId }: CreateCategoryButtonProps) {

  const { handleError } = useAppContext();
  const { categories } = useBoardCategoriesContext();

  const { addCategory } = useBoardCategoryEvents();

  async function createCategory() {
    try {
      const response = await apiFetch(`/api/boards/${boardId}/categories`, {
        method: "POST"
      });
      const newCategory: Category = await response.json();
      addCategory(newCategory);
    } catch (err) {
      // Another user may have created a category first in which case we can ignore the error.
      handleError(err, {
        ignore: [ ApiErrorCode.CATEGORY_LIMIT_EXCEEDED ]
      });
    }
  }

  const MAX_CATEGORIES = 30;
  return (
    <ActionButton
      className="flex items-center gap-2"
      onClick={createCategory}
      disabled={categories.length >= MAX_CATEGORIES}
    >
      <Plus className="w-5 h-5"/>
      <span>
        {categories.length >= MAX_CATEGORIES
          ? "Category Limit Reached"
          : "Create Category"}
      </span>
    </ActionButton>
  );
}