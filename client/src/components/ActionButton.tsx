

type ActionButtonProps = {
  className?: string;
  children: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
};

export default function ActionButton(
  {
    className,
    children,
    onClick,
    disabled = false
  }:
  ActionButtonProps
) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`rounded-lg border border-zinc-200 bg-white px-5 py-3 text-lg cursor-pointer
        transition
          hover:bg-zinc-200
          hover:border-zinc-300
        ${className}
      `}>
      {children}
    </button>
  );
}
