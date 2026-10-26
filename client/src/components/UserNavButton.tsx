import { useState } from "react";
import type { User } from "../types/user";
import type { Invite } from "../types/invite";
import { CircleUserRound, Mail } from "lucide-react";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";


type UserNavButtonProps = {
  user: User | null;
  invites: Invite[];
  setInvites: React.Dispatch<React.SetStateAction<Invite[]>>;
  setViewInvitesOpen: React.Dispatch<React.SetStateAction<boolean>>;
  setChangeUsernameOpen: React.Dispatch<React.SetStateAction<boolean>>;
};

export default function UserNavButton(
  {
    user,
    invites,
    setInvites,
    setViewInvitesOpen,
    setChangeUsernameOpen
  }:
  UserNavButtonProps
) {

  const { handleError } = useAppContext();

  const [isMenuOpen, setIsMenuOpen] = useState(false);

  async function fetchInvites() {
    try {
      const response = await apiFetch("/api/invites");
      const invites: Invite[] = await response.json();
      setInvites(invites);
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <div className="relative">
      <button
        type="button"
        className="flex items-center gap-2 text-slate-600 cursor-pointer"
        title={user?.username ?? "Guest"}
        onClick={() => {
          // TODO (maddie): we may want to synchronize opening.
          fetchInvites();
          setIsMenuOpen((open) => !open);
        }}
        aria-expanded={isMenuOpen}
        aria-haspopup="menu"
        >
        <CircleUserRound className="h-12 w-12" />
        <span className="text-lg font-medium">
          {user ? (user.username + "#" + user.id) : "Loading..."}
        </span>
      </button>

      {isMenuOpen && (
        <div
          role="menu"
          className="absolute right-0 top-full z-50 mt-2 w-64 rounded-xl border border-slate-200
                     bg-white p-2 shadow-lg text-lg"
        >
          <button
            type="button"
            role="menuitem"
            className="w-full rounded-lg p-3 text-left text-stale-700 cursor-pointer
                       transition hover:bg-sky-100"
            onClick={() => {
              setChangeUsernameOpen(true);
              setIsMenuOpen(false);
            }}
          >
              Change Username
          </button>
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setViewInvitesOpen(true);
              setIsMenuOpen(false);
            }}
            className="flex w-full items-center justify-between rounded-lg
                       p-3 text-left text-slate-700 cursor-pointer
                       transition hover:bg-sky-100"
            >
            <span className="flex items-center gap-3">
              <Mail className="h-5 w-5" />
              <span>Invites</span>
            </span>

            {invites.length > 0 ?
              (
                <span className="flex h-6 min-w-6 items-center justify-center
                                 rounded-full bg-blue-600 font-semibold text-white"
                >
                  {invites.length > 99 ? "99+" : invites.length}
                </span>
              ) : (
                <span
                    className="h-7 w-7 rounded-sm bg-slate-300 text-center"
                    title="No pending invites"
                >
                  0
                </span>
              )
            }
          </button>
        </div>
      )}
    </div>
  );
}
