
export type BackgroundType = "SOLID" | "GRADIENT";

export type Board = {
  id: number;
  boardOwnerUsername: string;
  boardOwnerId: number;
  backgroundType: BackgroundType;
  backgroundSolidColor: string;
  backgroundGradientStartColor: string;
  backgroundGradientEndColor: string;
};
