import { Plus } from "lucide-react";
import ActionButton from "./ActionButton";
import CategoryCard from "./CategoryCard";
import { useEffect, useState } from "react";
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


type BoardProps = {
  user: User;
  boardId: number;
  error: string;
};

export default function Board({ user, boardId, error }: BoardProps) {

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

  useEffect(() => {
    async function fetchCategoriesAndTasks() {
      try {
        const categoriesResponse = await apiFetch(`/api/boards/${boardId}/categories`);
        const categories: Category[] = await categoriesResponse.json();
        setCategories(categories);

        const tasksResponse = await apiFetch(`/api/boards/${boardId}/tasks`);
        const tasks: Task[] = await tasksResponse.json();

        const groupedTasks: Record<number, Task[]> = {};
        for (const category of categories) {
          groupedTasks[category.id] = [];
        }

        for (const task of tasks) {
          groupedTasks[task.categoryId].push(task);
        }
        setTasksByCategory(groupedTasks);
      } catch (err) {
        handleError(err);
      }
    }
    fetchCategoriesAndTasks();
  }, [boardId]);

  async function createCategory() {
    try {
      const response = await apiFetch(`/api/boards/${boardId}/categories`, {
        method: "POST"
      });
      const newCategory: Category = await response.json();
      setCategories((current) => [...current, newCategory]);
      setTasksByCategory((current) => ({...current, [newCategory.id]: [] }));
    } catch (err) {
      handleError(err);
    }
  }

  return (
    <>
      <div className="flex flex-col min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50">
        <div className="border-b border-zinc-300 bg-slate-50 px-8 py-6">
            <div className="flex w-full justify-between">
              <div className="flex gap-3">
                <ActionButton className="flex items-center gap-2" onClick={createCategory}>
                  <Plus className="w-5 h-5"/>
                  <span>Create Category</span>
                </ActionButton>
                <ActionButton onClick={() => setSendInvitesOpen(true)}>Invite</ActionButton>
                <ActionButton onClick={() => {}}>Change Background</ActionButton>
              </div>

              <UserNavButton
                user={user}
                invites={invites}
                setInvites={setInvites}
                setViewInvitesOpen={setViewInvitesOpen}
              />
          </div>
        </div>

        {/* categories*/}
        <div className="flex flex-1 items-start gap-4 overflow-x-scroll p-8">
        {categories.map((category, index) => (
          <CategoryCard
            key={category.id}
            categories={categories}
            category={category}
            prevCategoryId={index > 0 ? categories[index - 1].id : null}
            nextCategoryId={index < categories.length - 1 ? categories[index + 1].id : null}
            tasks={tasksByCategory[category.id] ?? []}
            tasksByCategory={tasksByCategory}
            setTasksByCategory={setTasksByCategory}
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
    </>
  );
}