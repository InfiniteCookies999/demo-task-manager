import { useEffect, useState } from "react";
import type { Category } from "../types/category";
import { CATEGORY_COLORS, SEPARATER_CATEGORY_BORDER_COLORS } from "../types/category";
import { Circle, CircleCheck, Plus, SquarePen } from "lucide-react";
import EditCategoryDialog from "./EditCategoryDialog";
import type { Task } from "../types/task";
import EditTaskDialog from "./EditTaskDialog";
import { useAppContext } from "../api/AppContext";
import { apiFetch } from "../api/apiFetch";

type CategoryCardProps = {
  category: Category;
  categories: Category[];
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  draggingCategoryId: number | null;
  setDraggingCategoryId: React.Dispatch<React.SetStateAction<number | null>>;
  dropTargetCategoryId: number | null;
  setDropTargetCategoryId: React.Dispatch<React.SetStateAction<number | null>>;
};

function HeaderButton(
  { className, children, onClick }: { className?: string, children: React.ReactNode, onClick: () => void }
) {
  return (
    <button
        className={`rounded-lg border border-zinc-300 bg-zinc-200 px-3 py-1
                    text-lg cursor-pointer
          transition
            hover:bg-sky-200
            hover:border-sky-300
          ${className}
        `}
        onClick={onClick}
        >
        {children}
    </button>
  );
}

export default function CategoryCard(
  {
    category,
    categories,
    setCategories,
    draggingCategoryId,
    setDraggingCategoryId,
    dropTargetCategoryId,
    setDropTargetCategoryId
  }:
  CategoryCardProps
) {

  const { handleError } = useAppContext();

  const [tasks, setTasks] = useState<Task[]>([]);

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isTaskEditOpen, setIsTaskEditOpen] = useState(false);
  const [editTask, setEditTask] = useState<Task | null>(null)


  // Load tasks for the category
  useEffect(() => {
    async function fetchTasks() {
      try {
        const response = await fetch(`/api/tasks?categoryId=${category.id}`);
        const tasks: Task[] = await response.json();
        setTasks(tasks);
      } catch (err) {
        handleError(err);
      }
    }
    fetchTasks();
  }, [category.id]);


  async function addTask() {
    try {
      const response = await apiFetch("/api/tasks", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          categoryId: category.id,
          name: "New Task"
        })
      });
      const newTask: Task = await response.json();
      setTasks((current) => [...current, newTask]);
    } catch (err) {
      handleError(err);
    }
  }

  async function markTaskCompleted(taskId: number, completed: boolean) {
    try {
      const newCompleted = !completed;
      await apiFetch(`/api/tasks/${taskId}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          completed: newCompleted
        })
      });
      setTasks((current) =>
        current.map((otherTask) => otherTask.id == taskId
          ? { ...otherTask, completed: newCompleted } : otherTask))
    } catch (err) {
      handleError(err);
    }
  }


  // Drag and drop functionality.
  // ----------------------------------

  function handleDragStart(
    e: React.DragEvent<HTMLDivElement>,
    id: number
  ) {
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", String(id));

    const elm = e.currentTarget.closest("[data-category-id]") as HTMLElement | null;

    if (elm) {

      // Create a clone of the element so we can modify the drag image.
      const clone = elm.cloneNode(true) as HTMLElement;

      clone.style.position = "absolute";
      // make sure it does not appear on the page since we are adding it to the DOM and only want it visible
      // through the drag image.
      clone.style.top = "-9999px";
      clone.style.left = "-9999px";
      clone.style.width = `${elm.offsetWidth}px`;
      clone.style.opacity = "0.9";
      clone.style.pointerEvents = "none";
      // clone.style.boxShadow = "0 25px 50px rgba(0, 0, 0, 0.5), 0 10px 20px rgba(0, 0, 0, 0.15)";
      clone.style.transform = "rotate(2deg)";


      document.body.appendChild(clone);

      e.dataTransfer.setDragImage(clone, e.nativeEvent.offsetX, e.nativeEvent.offsetY);

      // Once the browser actually transfers to using the clone we can get rid of it.
      requestAnimationFrame(() => {
        clone.remove();
      });
    }

    setDraggingCategoryId(id);
  }

  function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setDropTargetCategoryId(category.id);
  }

  async function updateCategoryOrder(categories: Category[]) {
    try {
      await apiFetch(`/api/categories/reorder`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          entries: categories.map(({ id }) => ({
            id
          }))
        })
      });
    } catch (err) {
      handleError(err);
    }
  }

  function handleDrop(
    e: React.DragEvent<HTMLDivElement>,
    targetId: number
  ) {
    e.preventDefault();

    const sourceId = Number(e.dataTransfer.getData("text/plain"));

    if (sourceId === targetId) {
      return;
    }

    const sourceIndex = categories.findIndex((category) => category.id === sourceId);
    const targetIndex = categories.findIndex((category) => category.id === targetId);

    if (sourceIndex === -1 || targetIndex === -1) {
      return;
    }

    const reordered = [...categories];
    const [moved] = reordered.splice(sourceIndex, 1);
    reordered.splice(targetIndex, 0, moved);

    const updated = reordered.map((category, index) => ({
      ...category,
      position: index,
    }));

    setDropTargetCategoryId(null);
    setCategories(updated);
    updateCategoryOrder(updated);
  }

  const isDragTarget = dropTargetCategoryId === category.id &&
                       draggingCategoryId !== null &&
                       draggingCategoryId !== category.id;

  return (
    <>
      {isDragTarget && (
          <div
            className="w-[400px] h-[190px] shrink-0 rounded-lg bg-zinc-300 border-b border-zinc-600"
            aria-hidden="true"
            onDragOver={handleDragOver}
            onDrop={(e) => handleDrop(e, category.id)}
          />
        )}

      <div
        data-category-id={category.id}
        className={
          `w-[400px] shrink-0 rounded-lg border border-zinc-300
          ${CATEGORY_COLORS[category.color]} p-4 shadow-sm
          ${isDragTarget ? "hidden"
                         : draggingCategoryId === category.id ? "opacity-30" : "opacity-100"}
          `
          }
        draggable
        onDragStart={(e) => handleDragStart(e, category.id)}
        onDragEnd={() => {
          setDraggingCategoryId(null);
          setDropTargetCategoryId(null);
        }}
        onDragOver={(e) => handleDragOver(e)}
        onDrop={(e) => handleDrop(e, category.id)}
        >

        {/* header */}
        <div className={`border-b ${SEPARATER_CATEGORY_BORDER_COLORS[category.color]} pb-2 mt-2`}>
          <div className="px-3 py-2 text-2xl">
            {category.name}
          </div>
        </div>
        <div className={`h-[60px] border-b ${SEPARATER_CATEGORY_BORDER_COLORS[category.color]} flex items-center gap-2`}>
          <HeaderButton className="flex items-center gap-1" onClick={addTask}>
            <Plus className="w-5 h-5"/>
            <span>Add Task</span>
          </HeaderButton>
          <HeaderButton className="flex items-center gap-2" onClick={() => setIsEditOpen(true)}>
            <SquarePen className="w-5 h-5" />
            <span>Edit</span>
          </HeaderButton>
        </div>
        {/* task drop area*/}
        <div className="p-4 flex flex-col gap-2">
            {tasks.map((task) => (
              <div
                key={task.id}
                className="
                  bg-zinc-50 shrink-0 rounded-lg border border-zinc-300 p-4 flex justify-between text-lg">

                <div className="flex items-start gap-2">
                  <button
                    type="button"
                    title={task.completed ? "Mark Not Complete" : "Mark Complete"}
                    aria-label="Mark Complete"
                    className="shrink-0 cursor-pointer"
                    onClick={() => markTaskCompleted(task.id, task.completed)}
                  >
                    {
                      task.completed
                        ? <CircleCheck className="w-5.5 h-5.5 shrink-0 translate-y-0.5 cursor-pointer fill-emerald-300"/>
                        : <Circle className="w-5.5 h-5.5 shrink-0 translate-y-0.5 cursor-pointer"/>
                    }
                  </button>

                  <span className={`min-w-0 break-words ${task.completed ? "text-[#777]" : ""}`}>{task.name}</span>
                </div>

                <SquarePen
                  className="cursor-pointer hover:bg-sky-200 rounded-lg shrink-0"
                  onClick={() => {
                    setEditTask(task);
                    setIsTaskEditOpen(true);
                  }}
                  >
                </SquarePen>
              </div>
            ))}
        </div>
      </div>
      {isEditOpen && (
        <EditCategoryDialog
          category={category}
          setCategories={setCategories}
          onClose={() => setIsEditOpen(false)}
        />
      )}
      {isTaskEditOpen && editTask && (
        <EditTaskDialog
          task={editTask}
          setTasks={setTasks}
          onClose={() => {
            setIsTaskEditOpen(false);
            setEditTask(null);
          }}/>
      )}
    </>
  );
}