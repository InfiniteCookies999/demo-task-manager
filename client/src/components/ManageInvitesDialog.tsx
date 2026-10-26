import { useEffect, useRef } from "react";
import DialogTitleAndClose from "./DialogTitleAndClose";
import { createPortal } from "react-dom";
import type { Invite } from "../types/invite";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";
import { useNavigate } from "react-router-dom";

type ManageInvitesDialogProps = {
  invites: Invite[];
  setInvites: React.Dispatch<React.SetStateAction<Invite[]>>;
  onClose: () => void;
};

export default function ManageInvitesDialog(
  {
    invites,
    setInvites,
    onClose
  }:
  ManageInvitesDialogProps
) {

  const { handleError } = useAppContext();

  const navigate = useNavigate();

  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function close() {
    dialogRef.current?.close();
    onClose();
  }

  async function declineInvite(inviteId: number) {
    setInvites((current) => current.filter((invite) => invite.id !== inviteId));
    try {
      await apiFetch(`/api/invites/${inviteId}`, { method: "DELETE" });
    } catch (err) {
      handleError(err);
    }
  }

  async function acceptInvite(inviteId: number, boardId: number) {
    try {
      await apiFetch(`/api/invites/accept/${inviteId}`, { method: "POST" });
      navigate(`boards/${boardId}`);
    } catch (err) {
      handleError(err);
    }
  }

  // TODO (maddie): we can only display so many at one time.
  return createPortal(
    <dialog
      ref={dialogRef}
      onClose={close}
      className="
        m-auto w-[600px] rounded-xl border border-zinc-300
        bg-white p-0 shadow-xl focus:outline-none
      "
    >
      <div className="p-6">

        <DialogTitleAndClose title={<span>Invites</span>} onClose={close} />

        {invites.map((invite) => (
          <div
            key={invite.id}
            className="flex justify-between mt-2 rounded-lg border border-zinc-200 bg-white shadow-sm p-3"
          >
            <div className="flex flex-col gap-2">
              <span className="text-xl">Invited to {invite.boardOwnerUsername}'s board</span>
              <span className="text-[#666]">Invited by: {invite.invitedByUsername + "#" + invite.id}</span>
            </div>

            <div className="flex flex-col gap-4">
              <button
                type="button"
                className="text-lg rounded-lg border border-zinc-200 bg-zinc-300 px-4 py-2
                  hover:bg-sky-200 cursor-pointer
                "
                onClick={() => acceptInvite(invite.id, invite.boardId)}
              >
                Accept
              </button>
              <button
                type="button"
                className="text-lg rounded-lg border border-zinc-200 bg-zinc-300 px-4 py-2
                  hover:bg-red-500 cursor-pointer
                "
                onClick={() => declineInvite(invite.id)}
              >
                Decline
              </button>
            </div>
          </div>
        ))}
        {
          invites.length == 0 ?
            <span className="text-lg">No invites</span> : null
        }

      </div>
    </dialog>,
    document.body
  );
}
