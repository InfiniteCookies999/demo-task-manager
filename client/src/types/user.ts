
export type User = {
  username: string;
  id: number;
  ownedBoardId: number;
};

export type UserInviteSearchResult = {
  id: number;
  username: string;
  alreadyInvited: boolean;
  alreadyMember: boolean;
};
