import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import DialogTitleAndClose from "./DialogTitleAndClose";
import type { User } from "../types/user";
import { useAppContext } from "../contexts/AppContext";
import { apiFetch } from "../api/apiFetch";
import { CheckCircle2 } from "lucide-react";

type ChangeUsernameDialogProps = {
  user: User;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
  onClose: () => void;
};

export default function ChangeUsernameDialog(
  { user, setUser, onClose }:
  ChangeUsernameDialogProps
) {

  const { handleError } = useAppContext();

  const dialogRef = useRef<HTMLDialogElement>(null);
  const originalUsername = useRef<string>(user.username);

  const [editUsername, setEditUsername] = useState(user.username);
  const [changeSuccessful, setChangeSuccessful] = useState(false);
  
  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  function close() {
    dialogRef.current?.close();
    onClose();
  }

  async function updateUsername() {
    if (!user) return;
    if (editUsername === originalUsername.current) return;
    try {
      await apiFetch("/api/user/change-username", {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: editUsername
        })
      });
      setUser((current) => {
        if (!current) return null;
        return {...current, username: editUsername };
      });
      setChangeSuccessful(true);
    } catch (err) {
      handleError(err);
    }
  }

  const isSubmittable = editUsername.length >= 2 && editUsername !== originalUsername.current;

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
          <DialogTitleAndClose title={<span>Change Username</span>} onClose={close} />

          <input
            value={editUsername}
            onChange={(e) => {
              setEditUsername(e.target.value.replace(/[^a-zA-Z0-9]/g, ""));
            }}
            minLength={2}
            maxLength={20}
            pattern="[a-zA-Z0-9]+"
            className="
              w-full rounded-lg border border-zinc-300 text-lg
              px-3 py-2 outline-none mb-4
              focus:border-blue-300
            "
          />

          <button
            type="button"
            className={`rounded-lg border border-zinc-200 bg-zinc-200 px-3 py-2 text-lg mb-3
                       ${isSubmittable ? "hover:bg-sky-200 cursor-pointer" : ""}
                       `}
            disabled={!isSubmittable}
            onClick={() => updateUsername()}
          >
            Submit Change
          </button>

          {changeSuccessful && (
            <p className="mb-4 flex items-center gap-2 text-sm font-medium text-emerald-700">
              <CheckCircle2 className="h-5 w-5 shrink-0" />
              Username successfully changed!
            </p>
          )}

        </div>
      </dialog>,
      document.body
  );
}