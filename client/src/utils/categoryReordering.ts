import type { Category } from "../types/category";

export function reorderCategories(categories: Category[], categoryIds: number[]): Category[] {

  const categoriesById = new Map(
    categories.map((category) => [category.id, category])
  );
  
  // Ignore events which may be out of date instead of attempting a partial order.
  if (categoriesById.size !== categories.length ||
      new Set(categoryIds).size !== categoryIds.length ||
      categoryIds.some((id) => !categoriesById.has(id))
  ) {
    return categories;
  }

  return categoryIds.map((id, index) => ({
    ...categoriesById.get(id)!,
    position: index
  }));
}
