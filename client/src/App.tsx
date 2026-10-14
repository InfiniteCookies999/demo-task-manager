import { useEffect, useState } from "react";
import type { Category } from "./types/category";
import ActionButton from "./components/ActionButton";
import CategoryCard from "./components/CategoryCard";

function App() {

  const [categories, setCategories] = useState<Category[]>([]);
  const [draggingCategoryId, setDraggingCategoryId] = useState<number | null>(null);

  useEffect(() => {
    async function fetchCategories() {
      const response = await fetch("/api/categories");
      // TODO (maddie): deal with errors
      const data: Category[] = await response.json();
      setCategories(data);
    }
    fetchCategories();
  }, []);

  async function createCategory() {
    const response = await fetch("/api/categories", {
      method: "POST"
    });
    // TODO (maddie): deal with errors.
    const newCategory: Category = await response.json();
    setCategories((current) => [...current, newCategory]);
  }

  return (
    <main className="flex flex-col min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">

        <div className="border-b border-zinc-300 bg-slate-50 px-8 py-6">
          <div className="max-w-5xl">
            <div className="flex gap-3">
              <ActionButton name="+ Create Category" onClick={createCategory}></ActionButton>
              <ActionButton name="Invite" onClick={() => {}}></ActionButton>
              <ActionButton name="Change Background" onClick={() => {}}></ActionButton>
            </div>
          </div>
        </div>

        {/* categories */}
        <div className="flex flex-1 items-start gap-4 overflow-x-scroll p-8">
          {categories.map((category) => (
            <CategoryCard
              key={category.id}
              categories={categories}
              category={category}
              setCategories={setCategories}
              draggingCategoryId={draggingCategoryId}
              setDraggingCategoryId={setDraggingCategoryId}
            ></CategoryCard>
          ))}
        </div>

      </main>
  )
}

export default App;
