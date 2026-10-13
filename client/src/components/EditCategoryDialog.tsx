import { useEffect, useRef, useState } from "react";
import { CATEGORY_COLORS, type Category } from "../types/category";
import { SquarePen, X } from "lucide-react";


type EditCategoryDialogProps = {
  category: Category;
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  onClose: () => void;
};

export default function EditCategoryDialog(
  {
    category,
    setCategories,
    onClose
  }:
  EditCategoryDialogProps
) {

  // TODO (maddie): sometimes when this is opened up it the exit button has a weird
  // red border.

  const [editName, setEditName] = useState(category.name);
  const [editColor, setEditColor] = useState(category.color);

  const dialogRef = useRef<HTMLDialogElement>(null);

  // Reset the form whenever the dialog is opened for a category.
  useEffect(() => {
    setEditName(category.name);
    setEditColor(category.color);
    dialogRef.current?.showModal();
  }, [category]);

  function close() {
    dialogRef.current?.close();
    onClose();
  }

  async function saveCategory() {
    if (editName === category.name &&
        editColor === category.color) {
      close();
      return;
    }

    setCategories((current) =>
      current.map((otherCategory) => otherCategory.id == category.id
        ? {...otherCategory, name: editName, color: editColor} : otherCategory)
    );

    const response = await fetch(`/api/categories/${category.id}`, {
      method: "PATCH",
      headers: {
          "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name: editName,
        color: editColor
      })
    });
    // TODO (maddie): deal with errors.
    close();
  }

  async function deleteCategory() {
    const response = await fetch(`/api/categories/${category.id}`, {
      method: "DELETE",
    });
    // TODO (maddie): deal with errors.
    setCategories((current) =>
      current.filter((otherCategory) => otherCategory.id !== category.id));
    close();
  }

  return (
    <dialog
      ref={dialogRef}
      onClose={close}
      className="
        m-auto w-[400px] rounded-xl border border-zinc-300
        bg-white p-0 shadow-xl
      "
    >
      <div className="p-6">

        <button
          type="button"
          onClick={close}
          className="
            absolute right-4 top-4
            rounded-lg p-1.5
            text-zinc-500
            hover:bg-zinc-100 hover:text-zinc-800
            cursor-pointer
          "
          aria-label="Close"
        >
          <X className="h-7 w-7" />
        </button>

        <h2 className="mb-6 text-2xl font-semibold flex items-center gap-3">
          <SquarePen></SquarePen>
          <span>Edit Category</span>
        </h2>

        {/* Name */}
        <div className="mb-3">
          <label
            htmlFor={`category-name-${category.id}`}
            className="mb-2 block text-xl font-medium text-zinc-700">
            Name
          </label>
          <input
            id={`category-name-${category.id}`}
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            maxLength={100}
            className="
              w-full rounded-lg border border-zinc-300 text-lg
              px-3 py-2 outline-none
              focus:border-blue-300
            "
          />
        </div>
        {/* Color */}
        <div className="mb-7">
          <label
            htmlFor={`category-name-${category.id}`}
            className="mb-2 block text-xl font-medium text-zinc-700">
            Color
          </label>

          <div className="flex gap-2">
            {
            (Object.keys(CATEGORY_COLORS) as (keyof typeof CATEGORY_COLORS)[])
              .map((color) => (
                <button
                  key={color}
                  className={
                    `w-9 h-9 ${CATEGORY_COLORS[color]} rounded-md border
                    ${
                      editColor === color
                        ? "border-blue-400 ring-2 ring-blue-400"
                        : "border-zinc-300 hover:border-zinc-500"
                    }
                    `}
                  onClick={() => setEditColor(color)}
                  >
                </button>
              ))
            }
          </div>
        </div>

        {/* Save/Delete buttons */}
        <div className="flex justify-between">
          <button
            type="button"
            onClick={saveCategory}
            className="text-lg rounded-lg bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 cursor-pointer"
          >
            Save
          </button>

          <button
            type="button"
            onClick={deleteCategory}
            className="text-lg rounded-lg bg-red-400 px-4 py-2 text-white hover:bg-red-500 cursor-pointer"
          >
            Delete
          </button>
        </div>
      </div>
    </dialog>
  );
}