import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "./DialogTitleAndClose";
import { type UserInviteSearchResult } from "../types/user";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";
import { Clock8, UserRoundPlus } from "lucide-react";
import UserSearch from "./UserSearch";

type SendInvitesDialogProps = {
  boardId: number;
  onClose: () => void;
};

export default function SendInvitesDialog({ boardId, onClose }: SendInvitesDialogProps) {

  const { handleError } = useAppContext();

  const [searchedUsername, setSearchedUsername] = useState("");
  const [foundUsers, setFoundUsers] = useState<UserInviteSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function close() {
    dialogRef.current?.close();
    onClose();
  }

  async function sendInvite(inviteUserId: number) {
    try {
      await apiFetch(`/api/boards/${boardId}/invites/send`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          userId: inviteUserId
        })
      });
      setFoundUsers((current) =>
        current.map((user) => (user.id === inviteUserId
          ? {...user, alreadyInvited: true}
          : {...user}))
      );
    } catch (err) {
      handleError(err);
    }
  }

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
        <DialogTitleAndClose title={<span>Send Invites</span>} onClose={close} />

        <UserSearch<UserInviteSearchResult>
          inputId="user-invite-search"
          setFoundUsers={setFoundUsers}
          setIsSearching={setIsSearching}
          searchedUsername={searchedUsername}
          setSearchedUsername={setSearchedUsername}
          headerText="Find Users by Username"
          searchEndpoint={
            (username) =>
              `/api/boards/${boardId}/invites/user-search?username=${encodeURIComponent(username)}`
          }
        />

        <div className="overflow-hidden">
          {searchedUsername.trim().length >= 2 ? (
            isSearching ? (
              <p className="px-4 py-3 text-sm text-zinc-500">Searching users...</p>
            ) : (
              foundUsers.length > 0 ? (
                foundUsers.map((user) => {

                  const statusMessage = user.alreadyMember
                    ? "Already a member"
                    : user.alreadyInvited
                      ? "Invite pending"
                      : "Invite";

                  const statusIcon = user.alreadyMember
                    ? <UserRoundPlus className="w-6 h-6 shrink-0 text-emerald-600"></UserRoundPlus>
                    : user.alreadyInvited
                      ? <Clock8 className="w-6 h-6 shrink-0 text-stone-500"></Clock8>
                      : null;

                  const disabled = user.alreadyInvited || user.alreadyMember;

                  return (
                    <button
                      key={user.id}
                      type="button"
                      onClick={() => {
                        if (user.alreadyInvited || user.alreadyMember) return;
                        sendInvite(user.id);
                      }}
                      className={`block w-full px-4 py-3 text-left
                          mt-2 rounded-lg border border-zinc-200 bg-white shadow-sm
                          ${
                            disabled
                              ? "bg-zinc-50"
                              : "cursor-pointer hover:bg-sky-100"
                          }
                        `}
                    >
                      <div className="flex items-center gap-2">
                        {statusIcon}
                        <span className="text-lg font-bold">
                          {statusMessage}
                        </span>
                        <span className="text-lg text-zinc-700">
                          {user.username + "#" + user.id}
                        </span>
                      </div>
                    </button>
                  );
                })
              ) : (
                <p className="px-4 py-3 text-sm text-zinc-600">No users found.</p>
              )
            )
          ) : (
            <p className="px-4 py-3 text-sm text-zinc-600">Type in a username of at least 2 characters.</p>
          )}
        </div>
      </div>
    </dialog>,
    document.body
  );
}
