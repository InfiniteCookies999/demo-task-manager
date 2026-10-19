
export const CATEGORY_COLORS = {
  RED: "bg-red-200",
  BLUE: "bg-blue-200",
  GREEN: "bg-green-200",
  YELLOW: "bg-yellow-200",
  PURPLE: "bg-purple-300",
  ORANGE: "bg-orange-300",
  WHITE: "bg-neutral-50",
  GRAY: "bg-zinc-100"
} as const;

export const SEPARATER_CATEGORY_BG_COLORS = {
  RED: "bg-zinc-500",
  BLUE: "bg-zinc-400",
  GREEN: "bg-zinc-400",
  YELLOW: "bg-zinc-400",
  PURPLE: "bg-zinc-600",
  ORANGE: "bg-zinc-500",
  WHITE: "bg-zinc-400",
  GRAY: "bg-zinc-400"
};

export const SEPARATER_CATEGORY_BORDER_COLORS = {
  RED: "border-zinc-500",
  BLUE: "border-zinc-400",
  GREEN: "border-zinc-400",
  YELLOW: "border-zinc-400",
  PURPLE: "border-zinc-600",
  ORANGE: "border-zinc-500",
  WHITE: "border-zinc-400",
  GRAY: "border-zinc-400"
};

export type CategoryColor = keyof typeof CATEGORY_COLORS;

export type Category = {
  id: number,
  boardId: number;
  name: string,
  color: CategoryColor,
  position: number
};
