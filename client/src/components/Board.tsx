import { CircleUserRound, Plus } from "lucide-react";
import ActionButton from "./ActionButton";
import CategoryCard from "./CategoryCard";
import { useEffect, useState } from "react";
import type { Category } from "../types/category";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../api/AppContext";
import type { User } from "../types/user";
import ErrorAlert from "./ErrorAlert";


type BoardProps = {
  user: User;
  boardId: number;
  error: string;
};

export default function Board({ user, boardId, error }: BoardProps) {

  const { handleError } = useAppContext();

  const [categories, setCategories] = useState<Category[]>([]);
  const [draggingCategoryId, setDraggingCategoryId] = useState<number | null>(null);
  const [dropTargetCategoryId, setDropTargetCategoryId] = useState<number | null>(null);

  useEffect(() => {
    async function fetchCategories() {
      try {
        const categoriesResponse = await apiFetch(`/api/boards/${boardId}/categories`);
        const categories: Category[] = await categoriesResponse.json();
        setCategories(categories);
      } catch (err) {
        handleError(err);
      }
    }
    fetchCategories();
  }, [boardId]);

  async function createCategory() {
    try {
      const response = await apiFetch(`/api/boards/${boardId}/categories`, {
        method: "POST"
      });
      const newCategory: Category = await response.json();
      setCategories((current) => [...current, newCategory]);
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="flex flex-col min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">

      <div className="border-b border-zinc-300 bg-slate-50 px-8 py-6">
          <div className="flex w-full justify-between">
            <div className="flex gap-3">
              <ActionButton className="flex items-center gap-2" onClick={createCategory}>
                <Plus className="w-5 h-5"/>
                <span>Create Category</span>
              </ActionButton>
              <ActionButton onClick={() => {}}>Invite</ActionButton>
              <ActionButton onClick={() => {}}>Change Background</ActionButton>
            </div>

            <div className="flex items-center gap-2 text-slate-600 cursor-pointer" title={user?.username ?? "Guest"}>
              <CircleUserRound className="h-12 w-12" />
              <span className="text-lg font-medium">
                {user?.username ?? "Loading..."}
              </span>
            </div>
        </div>
      </div>

      {/* categories*/}
      <div className="flex flex-1 items-start gap-4 overflow-x-scroll p-8">
      {categories.map((category) => (
        <CategoryCard
          key={category.id}
          categories={categories}
          category={category}
          setCategories={setCategories}
          draggingCategoryId={draggingCategoryId}
          setDraggingCategoryId={setDraggingCategoryId}
          dropTargetCategoryId={dropTargetCategoryId}
          setDropTargetCategoryId={setDropTargetCategoryId}
        ></CategoryCard>
      ))}
      </div>

      {/* TODO (maddie): ideally this ends up being abstracted out */}
      <ErrorAlert error={error}/>
    </div>
  );
}