import { useState } from "react";
import SendInvitesDialog from "../user/SendInvitesDialog";
import ChangeBackgroundDialog from "./ChangeBackgroundDialog";
import type { Board } from "../../types/board";
import ActionButton from "./ActionButton";

type BoardDialogsProps = {
  board: Board;
  setBoard: React.Dispatch<React.SetStateAction<Board | null>>;
};

export default function BoardDialogs(
  {
    board,
    setBoard
  }: BoardDialogsProps
) {

  const [sendInvitesOpen, setSendInvitesOpen] = useState(false);
  const [changeBackgroundOpen, setChangeBackgroundOpen] = useState(false);

  return (
    <>
      <ActionButton onClick={() => setSendInvitesOpen(true)}>
        Invite
      </ActionButton>
      <ActionButton onClick={() => setChangeBackgroundOpen(true)}>
        Change Background
      </ActionButton>

      {sendInvitesOpen &&
        <SendInvitesDialog boardId={board.id} onClose={() => setSendInvitesOpen(false)} />
      }
      {changeBackgroundOpen &&
        <ChangeBackgroundDialog
          board={board}
          setBoard={setBoard}
          onClose={() => setChangeBackgroundOpen(false)}
        />
      }
    </>
  );
}
