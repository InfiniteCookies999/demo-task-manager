import { useEffect, useRef, useState } from "react";
import { CATEGORY_COLORS, type Category } from "../types/category";
import { SquarePen, X } from "lucide-react";
import DeleteCollapsable from "./DeleteCollapsable";
import { useAppContext } from "../contexts/AppContext";
import { apiFetch } from "../api/apiFetch";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "./DialogTitleAndClose";


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

  const { handleError } = useAppContext();

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
      current.map((otherCategory) => otherCategory.id === category.id
        ? {...otherCategory, name: editName, color: editColor} : otherCategory)
    );

    try {
      await apiFetch(`/api/boards/${category.boardId}/categories/${category.id}`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name: editName,
          color: editColor
        })
      });
    } catch (err) {
      handleError(err);
    }
    close();
  }

  async function deleteCategory(deleteName: string) {
    if (deleteName !== editName) {
      return;
    }

    try {
      await apiFetch(`/api/boards/${category.boardId}/categories/${category.id}`, {
        method: "DELETE",
      });
      setCategories((current) =>
        current.filter((otherCategory) => otherCategory.id !== category.id));
    } catch (err) {
      handleError(err);
    }

    close();
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      onClose={close}
      className="
        m-auto w-[500px] rounded-xl border border-zinc-300
        bg-white p-0 shadow-xl
      "
    >
      <div className="p-6">

        <DialogTitleAndClose
          title={
            <>
              <SquarePen/>
              <span>Edit Category</span>
            </>
          }
          onClose={close}
        />

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
            onBlur={() => {
              if (editName.trim() === "") {
                setEditName(category.name);
              }
            }}
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
        <div className="mb-5">
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
        <button
          type="button"
          onClick={saveCategory}
          className="text-xl w-30 rounded-lg bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 cursor-pointer"
          >
          Save
        </button>
        <br/>

        <DeleteCollapsable
          className="mt-8"
          itemId={category.id}
          originalName={category.name}
          onDelete={(deleteName) => deleteCategory(deleteName)}
        />

      </div>
    </dialog>,
    document.body
  );
}