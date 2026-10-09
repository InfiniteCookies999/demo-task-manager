
export const CATEGORY_COLORS = {
  RED: "bg-red-100",
  BLUE: "bg-blue-100",
  GREEN: "bg-green-100",
  YELLOW: "bg-yellow-100",
  PURPLE: "bg-purple-100",
  ORANGE: "bg-orange-100",
  WHITE: "bg-white-100",
  GRAY: "bg-zinc-100",
  BLACK: "bg-black-100",
} as const;

export type CategoryColor = keyof typeof CATEGORY_COLORS;

export type Category = {
  id: number,
  name: string,
  color: CategoryColor,
  position: number
};
