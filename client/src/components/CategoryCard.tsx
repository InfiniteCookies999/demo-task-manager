import { useRef, useState } from "react";
import type { Category } from "../types/category";
import { CATEGORY_COLORS, SEPARATER_CATEGORY_BG_COLORS, SEPARATER_CATEGORY_BORDER_COLORS } from "../types/category";
import { SquarePen, X } from "lucide-react";
import EditCategoryDialog from "./EditCategoryDialog";

type CategoryCardProps = {
  category: Category;
  categories: Category[];
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  draggingCategoryId: number | null;
  setDraggingCategoryId: React.Dispatch<React.SetStateAction<number | null>>;
};

function HeaderButton({ children, onClick }: { children: React.ReactNode, onClick: () => void }) {
  return (
    <button
        className="rounded-lg border border-zinc-300 bg-zinc-200 px-3 py-1
                    text-lg cursor-pointer flex items-center gap-2
          transition
            hover:bg-sky-200
            hover:border-sky-300
        "
        onClick={onClick}
        >
        {children}
    </button>
  );
}

export default function CategoryCard(
  {
    category,
    categories,
    setCategories,
    draggingCategoryId,
    setDraggingCategoryId
  }:
  CategoryCardProps
) {

  const [isEditOpen, setIsEditOpen] = useState(false);

  function handleDragStart(
    e: React.DragEvent<HTMLDivElement>,
    id: number
  ) {
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", String(id));

    const elm = e.currentTarget.closest("[data-category-id]") as HTMLElement | null;

    if (elm) {

      // Create a clone of the element so we can modify the drag image.
      const clone = elm.cloneNode(true) as HTMLElement;

      clone.style.position = "absolute";
      // make sure it does not appear on the page since we are adding it to the DOM and only want it visible
      // through the drag image.
      clone.style.top = "-9999px";
      clone.style.left = "-9999px";
      clone.style.width = `${elm.offsetWidth}px`;
      clone.style.opacity = "0.9";
      clone.style.pointerEvents = "none";
      // clone.style.boxShadow = "0 25px 50px rgba(0, 0, 0, 0.5), 0 10px 20px rgba(0, 0, 0, 0.15)";
      clone.style.transform = "rotate(2deg)";


      document.body.appendChild(clone);

      e.dataTransfer.setDragImage(clone, e.nativeEvent.offsetX, e.nativeEvent.offsetY);

      // Once the browser actually transfers to using the clone we can get rid of it.
      requestAnimationFrame(() => {
        clone.remove();
      });
    }

    setDraggingCategoryId(id);
  }

  function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
  }

  async function updateCategoryOrder(categories: Category[]) {
    console.log("updating categories order");
    const response = await fetch(`/api/categories/reorder`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        entries: categories.map(({ id, position }) => ({
          id, position
        }))
      })
    });
    // TODO (maddie): handle errors
  }

  function handleDrop(
    e: React.DragEvent<HTMLDivElement>,
    targetId: number
  ) {
    e.preventDefault();

    const sourceId = Number(e.dataTransfer.getData("text/plain"));

    if (sourceId === targetId) {
      return;
    }

    const sourceIndex = categories.findIndex((category) => category.id === sourceId);
    const targetIndex = categories.findIndex((category) => category.id === targetId);

    if (sourceIndex === -1 || targetIndex === -1) {
      return;
    }

    const reordered = [...categories];
    const [moved] = reordered.splice(sourceIndex, 1);
    reordered.splice(targetIndex, 0, moved);

    const updated = reordered.map((category, index) => ({
      ...category,
      position: index,
    }));

    setCategories(updated);
    updateCategoryOrder(updated);
  }

  return (
    <>
      <div
        data-category-id={category.id}
        className={
          `w-[400px] shrink-0 rounded-lg border border-zinc-300
          ${CATEGORY_COLORS[category.color]} p-4 shadow-sm
          ${draggingCategoryId === category.id ? "opacity-30" : "opacity-100"}
          `
          }
        draggable
        onDragStart={(e) => handleDragStart(e, category.id)}
        onDragEnd={(e) => setDraggingCategoryId(null)}
        onDragOver={(e) => handleDragOver(e)}
        onDrop={(e) => handleDrop(e, category.id)}
        >

        {/* drag handle */}
        <div
          className={`
            flex h-8 cursor-grab items-center justify-center
            border-b ${SEPARATER_CATEGORY_BORDER_COLORS[category.color]} bg-black/5
            active:cursor-grabbing
          `}
          title="Drag to move category"
        >
          <div className="flex gap-1">
            <span className={`h-1 w-1 rounded-full ${SEPARATER_CATEGORY_BG_COLORS[category.color]}`} />
            <span className={`h-1 w-1 rounded-full ${SEPARATER_CATEGORY_BG_COLORS[category.color]}`} />
            <span className={`h-1 w-1 rounded-full ${SEPARATER_CATEGORY_BG_COLORS[category.color]}`} />
            <span className={`h-1 w-1 rounded-full ${SEPARATER_CATEGORY_BG_COLORS[category.color]}`} />
            <span className={`h-1 w-1 rounded-full ${SEPARATER_CATEGORY_BG_COLORS[category.color]}`} />
            <span className={`h-1 w-1 rounded-full ${SEPARATER_CATEGORY_BG_COLORS[category.color]}`} />
          </div>
        </div>

        {/* header */}
        <div className={`border-b ${SEPARATER_CATEGORY_BORDER_COLORS[category.color]} pb-2 mt-2`}>
          <div className="px-3 py-2 text-2xl">
            {category.name}
          </div>
        </div>
        <div className={`h-[60px] border-b ${SEPARATER_CATEGORY_BORDER_COLORS[category.color]} flex items-center gap-2`}>
          <HeaderButton onClick={() => {}}>+ Add Task</HeaderButton>
          <HeaderButton onClick={() => setIsEditOpen(true)}>
            <SquarePen className="h-5 w-5" />
            <span>Edit</span>
          </HeaderButton>
        </div>
        {/* task drop area*/}
        <div className="min-h-100 p-4">
            Tasks goes here
        </div>
      </div>
      {isEditOpen && (
        <EditCategoryDialog
          category={category}
          setCategories={setCategories}
          onClose={() => setIsEditOpen(false)}
        />
      )}
    </>
  );
}