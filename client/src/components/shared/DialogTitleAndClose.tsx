import { X } from "lucide-react";
import type { ReactNode } from "react";

type DialogTitleAndCloseProps = {
  title: ReactNode;
  onClose: () => void;
};

export default function DialogTitleAndClose({ title, onClose }: DialogTitleAndCloseProps) {
  return (
    <>
      <button
          type="button"
          onClick={onClose}
          className="
            absolute right-4 top-4
            rounded-lg p-1.5
            text-zinc-500
            hover:bg-zinc-100 hover:text-zinc-800
            focus:outline-none
            cursor-pointer
          "
          aria-label="Close"
        >
          <X className="h-7 w-7" />
        </button>

        <h2 className="mr-10 min-w-0 break-words mb-6 text-2xl font-semibold flex items-center gap-3">
          {title}
        </h2>
    </>
  );
}