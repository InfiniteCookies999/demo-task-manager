import { act, useEffect, useState } from "react";
import type { Category } from "../types/category";
import { CATEGORY_COLORS, SEPARATER_CATEGORY_BORDER_COLORS } from "../types/category";
import { Plus, SquarePen } from "lucide-react";
import EditCategoryDialog from "./EditCategoryDialog";
import type { Task, TaskDropTarget } from "../types/task";
import { useAppContext } from "../api/AppContext";
import { apiFetch } from "../api/apiFetch";
import TaskCard from "./TaskCard";
import createDragClone from "../utils/createDragClone";

type CategoryCardProps = {
  category: Category;
  categories: Category[];
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  tasks: Task[];
  prevCategoryId: number | null;
  nextCategoryId: number | null;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>;
  // Category dragging
  draggingCategory: Category | null;
  setDraggingCategory: React.Dispatch<React.SetStateAction<Category | null>>;
  dropTargetCategoryId: number | null;
  setDropTargetCategoryId: React.Dispatch<React.SetStateAction<number | null>>;
  // Task dragging
  draggingTask: Task | null;
  setDraggingTask: React.Dispatch<React.SetStateAction<Task | null>>;
  taskDropTarget: TaskDropTarget;
  setTaskDropTarget: React.Dispatch<React.SetStateAction<TaskDropTarget>>;
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

function DropArea(
  { category, handleDrop }:
  { category: Category, handleDrop: (e: React.DragEvent<HTMLDivElement>, targetId: number) => void}
) {
  return (
    <div
      className="w-[400px] h-[190px] shrink-0 rounded-lg bg-zinc-300 border-b border-zinc-600"
      aria-hidden="true"
      onDragOver={(e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
      }}
      onDrop={(e) => handleDrop(e, category.id)}
    />
  );
}

export default function CategoryCard(
  {
    category,
    categories,
    setCategories,
    tasks,
    prevCategoryId,
    nextCategoryId,
    setTasksByCategory,
    // Category dragging
    draggingCategory,
    setDraggingCategory,
    dropTargetCategoryId,
    setDropTargetCategoryId,
    // Task dragging
    draggingTask,
    setDraggingTask,
    taskDropTarget,
    setTaskDropTarget
  }:
  CategoryCardProps
) {

  const { handleError } = useAppContext();

  const [isEditOpen, setIsEditOpen] = useState(false);

  const setTasks: React.Dispatch<React.SetStateAction<Task[]>> =
    (action) =>
  {
    setTasksByCategory((current) => {
      const currentTasks = current[category.id] ?? [];

      const updatedTasks = typeof action === "function"
                          ? action(currentTasks)
                          : action;
      return {
        ...current,
        [category.id]: updatedTasks
      }
    });
  };

  async function addTask() {
    try {
      const response = await apiFetch(`/api/boards/${category.boardId}/tasks`, {
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


  // Drag and drop functionality.
  // ----------------------------------

  function handleDragStart(e: React.DragEvent<HTMLDivElement>) {
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", String(category.id));

    const elm = e.currentTarget.closest("[data-category-id]") as HTMLElement | null;

    if (elm) {

      // Create a clone of the element so we can modify the drag image.
      const clone = createDragClone(elm);
      document.body.appendChild(clone);

      e.dataTransfer.setDragImage(clone, e.nativeEvent.offsetX, e.nativeEvent.offsetY);

      // Once the browser actually transfers to using the clone we can get rid of it.
      requestAnimationFrame(() => {
        clone.remove();
      });
    }

    setDraggingCategory(category);
  }

  function handleDragEnter(e: React.DragEvent<HTMLDivElement>) {
    // Ignore drag events between children elements of the same card.
    if (e.relatedTarget instanceof Node &&
        e.currentTarget.contains(e.relatedTarget)) {
      return;
    }

    if (!draggingCategory) {
      if (draggingTask && tasks.length === 0) {
        setTaskDropTarget({ type: "category", id: category.id });
      }
      return;
    }
    if (draggingCategory.id === category.id) {
      return;
    }

    setDropTargetCategoryId((current) => {
      if (current === category.id) {
        // The user dragged back over the card that is the current drag
        // target. In this case we want to move to the next or previous
        // category since we place the drop area after/before depending
        // on its position relative to the card being dragged.
        if (draggingCategory.position < category.position) {
          return prevCategoryId;
        } else {
          return nextCategoryId;
        }
      }
      return category.id;
    });
  }

  async function updateCategoryOrder(categories: Category[]) {
    try {
      await apiFetch(`/api/boards/${category.boardId}/categories/reorder`, {
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
                       draggingCategory &&
                       draggingCategory.id !== category.id;

  const dragHasBeenMoved = draggingCategory &&
                           draggingCategory.id === category.id &&
                           dropTargetCategoryId !== null &&
                           dropTargetCategoryId !== category.id;

  return (
    <>
      {(isDragTarget && draggingCategory.position > category.position) &&
        <DropArea category={category} handleDrop={handleDrop} />}

      <div
        className={
          `w-[400px] shrink-0 rounded-lg border border-zinc-300
          ${CATEGORY_COLORS[category.color]} p-4 shadow-sm
          ${dragHasBeenMoved
              ? "hidden"
              : ((draggingCategory && draggingCategory.id === category.id) ? "opacity-30" : "opacity-100")}
          `
          }
        data-category-id={category.id}
        draggable
        onDragStart={(e) => handleDragStart(e)}
        onDragEnd={() => {
          setDraggingCategory(null);
          setDropTargetCategoryId(null);
        }}
        onDragEnter={(e) => handleDragEnter(e)}
        onDragOver={(e) => {
          e.preventDefault();
          e.dataTransfer.dropEffect = "move";
        }}
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
            <TaskCard
              key={task.id}
              category={category}
              task={task}
              tasks={tasks}
              setTasks={setTasks}
              setTasksByCategory={setTasksByCategory}
              draggingTask={draggingTask}
              setDraggingTask={setDraggingTask}
              taskDropTarget={taskDropTarget}
              setTaskDropTarget={setTaskDropTarget}
            />
          ))}
          {/* When task is null it handles the behavior for dropping task into an empty category. */}
          {tasks.length == 0 &&
            <TaskCard
              category={category}
              task={null}
              tasks={tasks}
              setTasks={setTasks}
              setTasksByCategory={setTasksByCategory}
              draggingTask={draggingTask}
              setDraggingTask={setDraggingTask}
              taskDropTarget={taskDropTarget}
              setTaskDropTarget={setTaskDropTarget}
            />
          }
        </div>
      </div>

      {isEditOpen && (
        <EditCategoryDialog
          category={category}
          setCategories={setCategories}
          onClose={() => setIsEditOpen(false)}
        />
      )}

      {(isDragTarget && draggingCategory.position < category.position) &&
        <DropArea category={category} handleDrop={handleDrop} />}
    </>
  );
}
