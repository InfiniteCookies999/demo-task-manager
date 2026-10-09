
export default function ActionButton({name, onClick}: { name: string, onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="rounded-lg border border-zinc-200 bg-white px-5 py-3 text-lg cursor-pointer
        transition
          hover:bg-zinc-200
          hover:border-zinc-300
      ">
      {name}
    </button>
  );
}
