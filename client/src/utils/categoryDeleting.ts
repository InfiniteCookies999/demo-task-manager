import type { Category } from "../types/category";

export function removeCategoryForDelete(categoryId: number, categories: Category[]) {
  return categories.filter((category) => category.id !== categoryId)
    .map((category, index) => ({
      ...category,
      position: index
    }));
}
