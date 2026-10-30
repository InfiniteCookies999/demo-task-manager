import { Plus } from "lucide-react";
import ActionButton from "./ActionButton";
import CategoryCard from "./CategoryCard";
import { useCallback, useEffect, useRef, useState } from "react";
import type { Category } from "../types/category";
import { apiFetch } from "../api/apiFetch";
import { useAppContext } from "../contexts/AppContext";
import type { User } from "../types/user";
import ErrorAlert from "./ErrorAlert";
import type { Task, TaskDropTarget } from "../types/task";
import UserNavButton from "./UserNavButton";
import SendInvitesDialog from "./SendInvitesDialog";
import ManageInvitesDialog from "./ManageInvitesDialog";
import type { Invite } from "../types/invite";
import ChangeUsernameDialog from "./ChangeUsernameDialog";
import { useBoardWebSocket } from "../hooks/useBoardWebSocket";
import { type BoardEvent } from "../types/event";
import { ApiError } from "../api/ApiError";
import { useBoardEvents } from "../hooks/useBoardEvents";
import ViewBoardsDialog from "./ViewBoardsDialog";

type BoardProps = {
  user: User;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
  boardId: number;
  error: string;
};

export default function Board({ user, setUser, boardId, error }: BoardProps) {

  const { handleError } = useAppContext();

  const [categories, setCategories] = useState<Category[]>([]);

  // Store the tasks by category in a Record so that when tasks are moved between
  // different categories they can be found by source/destination category id.
  const[tasksByCategory, setTasksByCategory] = useState<Record<number, Task[]>>({});

  const [draggingCategory, setDraggingCategory] = useState<Category | null>(null);
  const [dropTargetCategoryId, setDropTargetCategoryId] = useState<number | null>(null);

  const [draggingTask, setDraggingTask] = useState<Task | null>(null);
  const [taskDropTarget, setTaskDropTarget] = useState<TaskDropTarget>(null);

  // TODO (maddie): this is not a great place for this to go.
  // at least move it up one layer.
  const [invites, setInvites] = useState<Invite[]>([]);

  const [sendInvitesOpen, setSendInvitesOpen] = useState(false);
  const [viewInvitesOpen, setViewInvitesOpen] = useState(false);
  const [changeUsernameOpen, setChangeUsernameOpen] = useState(false);
  const [viewBoardsOpen, setViewBoardsOpen] = useState(false);

  const [initialStateLoaded, setInitialStateLoaded] = useState(false);

  const [notAuthorized, setNotAuthorized] = useState(false);

  const bufferedEvents = useRef<BoardEvent[]>([]);
  const isBufferingEvents = useRef(true);

  const {
    addCategory,
    addTask,
    applyBoardEvent
  } = useBoardEvents(setCategories, setTasksByCategory);

  const handleBoardEvent = useCallback((event: BoardEvent) => {
    if (isBufferingEvents.current) {
      bufferedEvents.current.push(event);
      return;
    }

    applyBoardEvent(event);
  }, [applyBoardEvent]);

  const { connected } = useBoardWebSocket(boardId, handleBoardEvent, !notAuthorized);

  useEffect(() => {
    // Ensure websocket connection before fetching main content otherwise events may happen
    // while not connected leading to missed events.
    if (!connected) {
      return;
    }

    isBufferingEvents.current = true;
    bufferedEvents.current = [];

    async function fetchCategoriesAndTasks() {
      try {
        const categoriesResponse = await apiFetch(`/api/boards/${boardId}/categories`);
        const categories: Category[] = await categoriesResponse.json();

        const tasksResponse = await apiFetch(`/api/boards/${boardId}/tasks`);
        const tasks: Task[] = await tasksResponse.json();

        const groupedTasks: Record<number, Task[]> = {};
        for (const category of categories) {
          groupedTasks[category.id] = [];
        }

        for (const task of tasks) {
          groupedTasks[task.categoryId].push(task);
        }

        setCategories(categories);
        setTasksByCategory(groupedTasks);

        // Apply the events that took place during the fetch.
        const events = bufferedEvents.current;
        bufferedEvents.current = [];
        isBufferingEvents.current = false;

        for (const event of events) {
          applyBoardEvent(event);
        }

        setInitialStateLoaded(true);

      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          setNotAuthorized(true);
          return;
        }
        handleError(err);
      }
    }
    fetchCategoriesAndTasks();
  }, [connected, boardId, applyBoardEvent]);

  async function createCategory() {
    try {
      const response = await apiFetch(`/api/boards/${boardId}/categories`, {
        method: "POST"
      });
      const newCategory: Category = await response.json();
      addCategory(newCategory);
    } catch (err) {
      handleError(err);
    }
  }

  if (notAuthorized) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="rounded-lg bg-white p-8 text-center shadow-md">
          <h1 className="mb-2 text-xl font-semibold text-gray-900">
            Access Denied
          </h1>
          <p className="text-gray-600">
            You are not authorized to access this board.
          </p>
        </div>
      </div>
    );
  }

  const MAX_CATEGORIES = 30;

  return (
    <>
      <div className="flex flex-col min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">
        <div className="border-b border-zinc-300 bg-slate-50 px-8 py-6">
            <div className="flex w-full justify-between">
              <div className="flex gap-3">
                {initialStateLoaded && (
                  <>
                    <ActionButton
                      className="flex items-center gap-2"
                      onClick={createCategory}
                      disabled={categories.length >= MAX_CATEGORIES}
                    >
                      <Plus className="w-5 h-5"/>
                      <span>
                        {categories.length >= MAX_CATEGORIES
                          ? "Category Limit Reached"
                          : "Create Category"}
                      </span>
                    </ActionButton>
                    <ActionButton onClick={() => setSendInvitesOpen(true)}>Invite</ActionButton>
                    <ActionButton onClick={() => {}}>Change Background</ActionButton>
                  </>
                )}
              </div>

              <UserNavButton
                user={user}
                invites={invites}
                setInvites={setInvites}
                setViewInvitesOpen={setViewInvitesOpen}
                setChangeUsernameOpen={setChangeUsernameOpen}
                setViewBoardsOpen={setViewBoardsOpen}
              />
          </div>
        </div>

        {/* categories*/}
        <div className="flex flex-1 items-start gap-4 overflow-x-scroll p-8">
        {initialStateLoaded && categories.map((category, index) => (
          <CategoryCard
            key={category.id}
            categories={categories}
            category={category}
            prevCategoryId={index > 0 ? categories[index - 1].id : null}
            nextCategoryId={index < categories.length - 1 ? categories[index + 1].id : null}
            tasks={tasksByCategory[category.id] ?? []}
            tasksByCategory={tasksByCategory}
            setTasksByCategory={setTasksByCategory}
            addTask={addTask}
            setCategories={setCategories}
            draggingCategory={draggingCategory}
            setDraggingCategory={setDraggingCategory}
            dropTargetCategoryId={dropTargetCategoryId}
            setDropTargetCategoryId={setDropTargetCategoryId}
            draggingTask={draggingTask}
            setDraggingTask={setDraggingTask}
            taskDropTarget={taskDropTarget}
            setTaskDropTarget={setTaskDropTarget}
          ></CategoryCard>
        ))}
        </div>

        {/* TODO (maddie): ideally this ends up being abstracted out */}
        <ErrorAlert error={error}/>
      </div>
      {sendInvitesOpen &&
        <SendInvitesDialog boardId={boardId} onClose={() => setSendInvitesOpen(false)} />
      }
      {viewInvitesOpen &&
        <ManageInvitesDialog
          invites={invites}
          setInvites={setInvites}
          onClose={() => {setViewInvitesOpen(false)}}
        />
      }
      {changeUsernameOpen &&
        <ChangeUsernameDialog user={user} setUser={setUser} onClose={() => setChangeUsernameOpen(false)} />
      }
      {viewBoardsOpen &&
        <ViewBoardsDialog user={user} onClose={() => setViewBoardsOpen(false)} />
      }
    </>
  );
}
