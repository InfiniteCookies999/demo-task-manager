import { useEffect, useState } from "react";
import type { Task, TaskAssignee, TaskAssigneeSearchResult } from "../../types/task";
import UserSearch from "../shared/UserSearch";
import { apiFetch } from "../../api/apiFetch";
import { useAppContext } from "../../contexts/AppContext";
import { ChevronDown } from "lucide-react";


type EditTaskAssigneesProps = {
  boardId: number;
  task: Task;
  originalAssigneesIdsRef: React.RefObject<number[] | null>;
  assignees: TaskAssignee[];
  setAssignees: React.Dispatch<React.SetStateAction<TaskAssignee[]>>;
};


export default function EditTaskAssignees(
  {
    boardId,
    task,
    originalAssigneesIdsRef,
    assignees,
    setAssignees
  }: EditTaskAssigneesProps
) {

  const { handleError } = useAppContext();

  const [showEditAssignees, setShowEditAssignees] = useState(false);

  const [loading, setLoading] = useState(true);
  const [searchedAssigneeUsername, setSearchedAssigneeUsername] = useState("");
  const [searchedAssignees, setSearchedAssignees] = useState<TaskAssigneeSearchResult[]>([]);
  const [isAssigneeSearching, setIsAssigneeSearching] = useState(false);

  useEffect(() => {
    async function fetchAssignees() {
      try {
        const response = await apiFetch(`/api/boards/${boardId}/tasks/${task.id}/assignees`);
        const assignees: TaskAssignee[] = (await response.json()).map((assignee: TaskAssignee) => ({
          ...assignee,
          assigned: true
        }));
        originalAssigneesIdsRef.current = assignees.map((assignee) => assignee.id);
        setAssignees(assignees);
        setLoading(false);
      } catch (err) {
        handleError(err);
      }
    }

    fetchAssignees();
  }, [task.id, boardId, setLoading, setAssignees]);


  async function assignToTask(userId: number, username: string) {
    setAssignees((current) => [...current, { id: userId, username, assigned: true }]);
    setSearchedAssignees((current) => current.map((user) => ({
      ...user, alreadyAssigned: user.id === userId ? true : user.alreadyAssigned
    })));
  }

  async function toggleAssignment(userId: number, assigned: boolean) {
    setAssignees((current) => current.map((user) => ({
      ...user, assigned: user.id === userId ? assigned : user.assigned
    })));
    setSearchedAssignees((current) => current.map((user) => ({
      ...user, alreadyAssigned: user.id === userId ? assigned : user.alreadyAssigned
    })));
  }

  if (loading) {
    <div className="mt-4">
      Loading assignees...
    </div>
  }

  return (
    <div className="mt-4">
      <button
        className="flex cursor-pointer text-xl"
        type="button"
        onClick={() => setShowEditAssignees((current) => !current)}
        >
        <ChevronDown className="h-7 w-7"/>
        <span>Assignees</span>
      </button>


      <div className={`${showEditAssignees ? "" : "hidden"} overflow-hidden mt-4`}>

        <div className="mb-4">
          {assignees.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {assignees.map((assignee) => (
                <label
                  key={assignee.id}
                  className={`
                    flex items-center gap-2 rounded-lg border px-3 py-2
                    ${assignee.assigned ? "bg-blue-50 border-blue-200" : "border-zinc-300"}
                    cursor-pointer select-none
                    hover:bg-blue-100
                  `}
                >
                  <input
                    type="checkbox"
                    checked={assignee.assigned}
                    onChange={() => {
                      const assigned: boolean = assignee.assigned!;
                      toggleAssignment(assignee.id, !assigned);
                    }}
                    className={`h-4 w-4 ${assignee.assigned ? "accent-blue-600" : ""} cursor-pointer`}
                  />

                  <span className={`text-sm font-medium ${assignee.assigned ? "text-blue-800" : ""}`}>
                    {assignee.username} # {assignee.id}
                  </span>
                </label>
              ))}
            </div>
          ) : (
            <p className="text-sm text-zinc-500 py-2">
              No assignees.
            </p>
          )}
        </div>

        <UserSearch<TaskAssigneeSearchResult>
          inputId="task-assignee-search"
          setFoundUsers={setSearchedAssignees}
          setIsSearching={setIsAssigneeSearching}
          searchedUsername={searchedAssigneeUsername}
          setSearchedUsername={setSearchedAssigneeUsername}
          headerText="Search board members by username"
          searchEndpoint={
            (username) =>
              `/api/boards/${boardId}/tasks/${task.id}/search-assignees?search=${encodeURIComponent(username)}`
          }
        />

        <div className="mb-4">
          {searchedAssigneeUsername.trim().length >= 2 ? (
            <>
              {isAssigneeSearching && searchedAssignees.length === 0 &&
                <p className="px-4 py-3 text-sm text-zinc-500">
                  Searching users...
                </p>}

              {searchedAssignees.length > 0 ? (
                <div className="mt-2">
                  {searchedAssignees.map((user) => (
                    <button
                      key={user.id}
                      type="button"
                      disabled={user.alreadyAssigned}
                      onClick={() => assignToTask(user.id, user.username)}
                      className={`
                        flex w-full items-center justify-between
                        mt-2 rounded-lg border border-zinc-200
                        px-4 py-2 text-left
                        ${
                          user.alreadyAssigned
                            ? "bg-zinc-50 text-zinc-400"
                            : "bg-white hover:bg-sky-100 cursor-pointer"
                        }
                      `}
                    >
                      <span className="text-lg font-medium">
                        {user.username}#{user.id}
                      </span>

                      <span className={`text-md font-medium ${
                        user.alreadyAssigned
                          ? "text-zinc-400"
                          : "text-blue-600"
                      }`}>
                        {user.alreadyAssigned ? "Already assigned" : "Assign"}
                      </span>
                    </button>
                  ))}
                </div>
              ) : (
                <p className="px-4 pb-2 text-sm text-zinc-500">
                  No users found.
                </p>
              )}
            </>
          )
          : <p className="px-4 pb-2 text-sm text-zinc-600">Type in a username of at least 2 characters.</p>
          }
        </div>
      </div>
    </div>
  );
}