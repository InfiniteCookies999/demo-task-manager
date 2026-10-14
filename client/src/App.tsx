import { useEffect, useState } from "react";
import type { Category } from "./types/category";
import ActionButton from "./components/ActionButton";
import CategoryCard from "./components/CategoryCard";
import { Plus } from "lucide-react";
import { apiFetch } from "./api/apiFetch";
import ErrorAlert from "./components/ErrorAlert";
import useApiError from "./hooks/apiError";
import { AppContext } from "./api/AppContext";


function App() {

  const [categories, setCategories] = useState<Category[]>([]);
  const [draggingCategoryId, setDraggingCategoryId] = useState<number | null>(null);
  const { error, handleError } = useApiError();

  useEffect(() => {
    async function fetchCategories() {
      try {
        const response = await apiFetch("/api/categories");
        const categories: Category[] = await response.json();
        setCategories(categories);
      } catch (err) {
        handleError(err);
      }
    }
    fetchCategories();
  }, []);

  async function createCategory() {
    try {
      const response = await apiFetch("/api/categories", {
        method: "POST"
      });
      const newCategory: Category = await response.json();
      setCategories((current) => [...current, newCategory]);
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <AppContext.Provider value={{handleError}}>
      <main className="flex flex-col min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">

        <div className="border-b border-zinc-300 bg-slate-50 px-8 py-6">
          <div className="max-w-5xl">
            <div className="flex gap-3">
              <ActionButton className="flex items-center gap-2" onClick={createCategory}>
                <Plus className="w-5 h-5"/>
                <span>Create Category</span>
              </ActionButton>
              <ActionButton onClick={() => {}}>Invite</ActionButton>
              <ActionButton onClick={() => {}}>Change Background</ActionButton>
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

        <ErrorAlert error={error}/>
      </main>
    </AppContext.Provider>
  );
}

export default App;
