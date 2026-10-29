import { useCallback } from "react";
import type { Category } from "../types/category";
import createDragClone from "../utils/createDragClone";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";


type UseCategoryDragProps = {
  boardId: number;
  categories: Category[];
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  draggingCategory: Category | null;
  setDraggingCategory: React.Dispatch<React.SetStateAction<Category | null>>;
  setDropTargetCategoryId: React.Dispatch<React.SetStateAction<number | null>>;
};


export function useCategoryDrag(
  {
    boardId,
    categories,
    setCategories,
    draggingCategory,
    setDraggingCategory,
    setDropTargetCategoryId
  }:
  UseCategoryDragProps
) {

  const { handleError } = useAppContext();

  const clearDragState = useCallback(() => {
    setDraggingCategory(null);
    setDropTargetCategoryId(null);
  }, [setDraggingCategory, setDropTargetCategoryId]);

  // Handle when a category is clicked on to begin dragging.
  //
  const handleDragStart = useCallback(
    (e: React.DragEvent<HTMLDivElement>, category: Category) => {
      // Prevent dragging when dialogs are open.
      if (document.querySelector("dialog:modal")) {
        e.preventDefault();
        return;
      }

      e.dataTransfer.effectAllowed = "move";
      e.dataTransfer.setData("text/plain", String(category.id));

      const elm = e.currentTarget.closest("[data-category-id]") as HTMLElement | null;

      if (elm) {
        const clone = createDragClone(elm);
        document.body.appendChild(clone);

        e.dataTransfer.setDragImage(clone, e.nativeEvent.offsetX, e.nativeEvent.offsetY);

        // Once the browser actually transfers to using the clone we can get rid of it.
        requestAnimationFrame(() => {
          clone.remove();
        });
      }

      setDraggingCategory(category);
    },
    [setDraggingCategory]
  );

  // Handles when a category enters another category to visually indicate
  // the new order that would be applied to the categories if the category
  // was dropped.
  //
  const handleDragEnter = useCallback(
    (
      e: React.DragEvent<HTMLDivElement>,
      category: Category,
      prevCategoryId: number | null,
      nextCategoryId: number | null
    ) => {
      // Ignore drag events between children elements of the same card.
      if (e.relatedTarget instanceof Node &&
          e.currentTarget.contains(e.relatedTarget)) {
        return;
      }

      if (!draggingCategory) {
        // Task dragging behavior is found in useCategoryTaskDrag.ts
        return;
      }
      if (draggingCategory.id === category.id) {
        return;
      }

      setDropTargetCategoryId((current) => {
        if (current === category.id) {
          // The user dragged back over the card that is the current drag
          // target. In this case we want to move to the next or previous
          // category since we place the drop area after/before depending
          // on its position relative to the card being dragged.
          return draggingCategory.position < category.position
            ? prevCategoryId
            : nextCategoryId;
        }

        return category.id;
      });
    },
    [draggingCategory, setDropTargetCategoryId]
  );

  // Handles when a category is dropped into another category.
  //
  async function updateCategoryOrder(boardId: number, categories: Category[]) {
    return apiFetch(`/api/boards/${boardId}/categories/reorder`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        categoryIds: categories.map(({ id }) => id)
      })
    });
  }

  const handleDrop = useCallback(
    async (
      e: React.DragEvent<HTMLDivElement>,
      targetId: number
    ) => {
      e.preventDefault();

      if (!draggingCategory) {
        // Task dragging behavior is found in useCategoryTaskDrag.ts
        return;
      }

      const sourceId = Number(e.dataTransfer.getData("text/plain"));

      if (sourceId !== draggingCategory.id || sourceId === targetId) {
        clearDragState();
        return;
      }

      const sourceIndex = categories.findIndex((category) => category.id === sourceId);
      const targetIndex = categories.findIndex((category) => category.id === targetId);

      if (sourceIndex === -1 || targetIndex === -1) {
        clearDragState();
        return;
      }

      // Reorder categories locally.
      const reordered = [...categories];
      const [moved] = reordered.splice(sourceIndex, 1);
      reordered.splice(targetIndex, 0, moved);

      const updated = reordered.map((category, index) => ({
        ...category,
        position: index,
      }));

      // Optimistically update. This could result in order mismatch in the case of multiple
      // drags but waiting on the server response in this case could lead to a poor experience.
      setCategories(updated);
      clearDragState();

      try {
        await updateCategoryOrder(boardId, updated);
      } catch (err) {
        handleError(err);
      }
    },
    [ boardId, categories, draggingCategory, setCategories, clearDragState ]
  );

  return {
    clearDragState,
    handleDragStart,
    handleDragEnter,
    handleDrop
  };
}