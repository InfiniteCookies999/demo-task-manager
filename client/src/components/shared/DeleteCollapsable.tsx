import { ChevronDown } from "lucide-react";
import { useState } from "react";

type DeleteCollapsableProps = {
  className: string;
  itemId: number,
  originalName: string;
  onDelete: (deleteName: string) => void;
};


export default function DeleteCollapsable(
  { className, itemId, originalName, onDelete }: DeleteCollapsableProps
) {
  
  const [showDeleteSection, setShowDeleteSection] = useState(false);
  const [deleteName, setDeleteName] = useState("");

  return (
    <div className={className}>
      <button
        className="flex cursor-pointer text-xl"
        type="button"
        onClick={() => setShowDeleteSection((current) => !current)}
        >
        <ChevronDown className="h-7 w-7"/>
        <span>Delete</span>
      </button>
      <div className={`${showDeleteSection ? "" : "hidden"} overflow-hidden mt-4`}>
        <label
          htmlFor={`category-delete-name-${itemId}`}
          className="mb-2 block text-xl font-medium text-zinc-700">
          Type Name to delete
        </label>
        <input
          id={`category-delete-name-${itemId}`}
          maxLength={100}
          className="
            w-full rounded-lg border border-zinc-300 text-lg
            px-3 py-2 outline-none
            focus:border-blue-300
          "
          value={deleteName}
          onChange={(e) => {setDeleteName(e.target.value)}}
        />
        <button
          type="button"
          onClick={() => onDelete(deleteName)}
          className={
            `text-xl w-30 rounded-lg px-4 py-2 text-white mt-3
            ${deleteName === originalName ? "bg-red-500 hover:bg-red-600 cursor-pointer" : "bg-zinc-500"}
            `}
          >
          Delete
        </button>
      </div>
    </div>
  );
} 