import type { Category } from "../types/category";

export function reorderCategories(categories: Category[], categoryIds: number[]): Category[] {

  const categoriesById = new Map(
    categories.map((category) => [category.id, category])
  );
  
  // Performing a partial reorder on the categories. A partial reorder is quite
  // important since the client may add a new category then the server may reply
  // with a re-order. In this case it still needs to re-order the categories the
  // server does currently know about to keep things ordered.
  const orderedIds = new Set<number>();
  const reordered = categoryIds
    .filter((id) => {
      if (!categoriesById.has(id) || orderedIds.has(id)) {
        return false;
      }

      orderedIds.add(id);
      return true;
    })
    .map((id) => categoriesById.get(id)!);

  // Keep the remaining categories at the end in their current local order.
  const remaining: Category[] = categories.filter((category) => !orderedIds.has(category.id));

  return [...reordered, ...remaining].map((category, index) => ({
    ...category,
    position: index
  }));
}
