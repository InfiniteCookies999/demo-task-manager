

type SaveAndCancelButtonsProps = {
  onSave: () => void;
  onCancel: () => void;
};

export default function SaveAndCancelButtons({ onSave, onCancel }: SaveAndCancelButtonsProps) {
  return (
    <div className="flex gap-4 mt-7">
      <button
        type="button"
        className="text-xl w-30 rounded-lg bg-blue-500 px-4 py-2 text-white
                    hover:bg-blue-600 cursor-pointer"
        onClick={onSave}
      >
        Save
      </button>
      <button
        type="button"
        className="text-xl w-30 rounded-lg bg-red-400 px-4 py-2 text-white
                    hover:bg-red-500 cursor-pointer"
        onClick={onCancel}
      >
        Cancel
      </button>
    </div>
  )
}