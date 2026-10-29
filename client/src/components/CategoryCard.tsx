import { useState } from "react";
import type { Category } from "../types/category";
import { CATEGORY_COLORS, SEPARATER_CATEGORY_BORDER_COLORS } from "../types/category";
import { Plus, SquarePen } from "lucide-react";
import EditCategoryDialog from "./EditCategoryDialog";
import { MAX_TASKS, type Task, type TaskDropTarget } from "../types/task";
import { useAppContext } from "../contexts/AppContext";
import { apiFetch } from "../api/apiFetch";
import TaskCard from "./TaskCard";
import { useCategoryDrag } from "../hooks/useCategoryDrag";
import { useCategoryTaskDrag } from "../hooks/useCategoryTaskDrag";

type CategoryCardProps = {
  category: Category;
  categories: Category[];
  setCategories: React.Dispatch<React.SetStateAction<Category[]>>;
  tasks: Task[];
  prevCategoryId: number | null;
  nextCategoryId: number | null;
  tasksByCategory: Record<number, Task[]>;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>;
  addTask: (task: Task) => void;
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
  {
    className,
    children,
    onClick,
    disabled = false
  }: {
    className?: string,
    children: React.ReactNode,
    onClick: () => void,
    disabled?: boolean
  }
) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`rounded-lg border border-zinc-300 bg-zinc-200 px-3 py-1
                  text-lg cursor-pointer
        transition
          hover:bg-sky-200
          hover:border-sky-300
        ${className}
      `}
      >
      {children}
    </button>
  );
}

function DropArea(
  {
    category,
    handleDrop,
    setDropTargetCategoryId
  }:
  {
    category: Category,
    handleDrop: (e: React.DragEvent<HTMLDivElement>, targetId: number) => void,
    setDropTargetCategoryId: React.Dispatch<React.SetStateAction<number | null>>
  }
) {
  return (
    // Ensuring the height stretches to the bottom of the viewport so that it does not accidently
    // think we are no longer dragging over the drop location.
    <div
      className="w-[400px] shrink-0 min-h-[calc(100vh-190px)]"
      onDragOver={(e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
      }}
      onDrop={(e) => handleDrop(e, category.id)}
      onDragLeave={(e) => {
        // Ignore drag events between children elements of the same card.
        if (e.relatedTarget instanceof Node &&
            e.currentTarget.contains(e.relatedTarget)) {
          return;
        }
        setDropTargetCategoryId(null);
      }}
      >
      <div
        className="w-[400px] h-[190px] shrink-0 rounded-lg bg-zinc-300 border-b border-zinc-600"
        aria-hidden="true"
      />
    </div>
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
    tasksByCategory,
    setTasksByCategory,
    addTask,
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

  async function updateAndAddTask() {
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
      addTask(newTask);
    } catch (err) {
      handleError(err);
    }
  }

  // Drag and drop functionality.

  const {
    clearDragState,
    handleDragStart,
    handleDragEnter,
    handleDrop
  } = useCategoryDrag({
    boardId: category.boardId,
    categories,
    setCategories,
    draggingCategory,
    setDraggingCategory,
    setDropTargetCategoryId
  });

  const {
    handleTaskDragEnter,
    handleTaskDrop
  } = useCategoryTaskDrag({
    boardId: category.boardId,
    setDraggingTask,
    setTaskDropTarget,
    draggingCategory,
    tasks,
    draggingTask,
    taskDropTarget,
    tasksByCategory,
    setTasksByCategory
  });

  const isDragTarget = dropTargetCategoryId === category.id &&
                       draggingCategory &&
                       draggingCategory.id !== category.id;

  const dragHasBeenMoved = draggingCategory &&
                           draggingCategory.id === category.id &&
                           dropTargetCategoryId !== null &&
                           dropTargetCategoryId !== category.id;

  const highlightAsDropTarget = taskDropTarget &&
                                ((taskDropTarget.type === "category" &&
                                 taskDropTarget.id === category.id) ||
                                 (taskDropTarget.type === "task" &&
                                  taskDropTarget.categoryId === category.id
                                ));

  return (
    <>
      {(isDragTarget && draggingCategory.position > category.position) &&
        <DropArea category={category} handleDrop={handleDrop} setDropTargetCategoryId={setDropTargetCategoryId} />}

      <div
        className={
          `w-[400px] shrink-0 rounded-lg border
          ${highlightAsDropTarget ? "ring-1 ring-blue-500/2 border-blue-500" : "border-zinc-300"}
          ${CATEGORY_COLORS[category.color]} p-4 shadow-sm
          ${dragHasBeenMoved
              ? "hidden"
              : ((draggingCategory && draggingCategory.id === category.id) ? "opacity-30" : "opacity-100")}
          `
          }
        data-category-id={category.id}
        draggable
        onDragStart={(e) => handleDragStart(e, category)}
        onDragEnd={clearDragState}
        onDragEnter={(e) => {
          handleDragEnter(e, category, prevCategoryId, nextCategoryId);
          handleTaskDragEnter(e, category);
        }}
        onDragOver={(e) => {
          e.preventDefault();
          e.dataTransfer.dropEffect = "move";
        }}
        onDrop={(e) => {
          handleDrop(e, category.id);
          handleTaskDrop(e, category.id);
        }}
        onDragLeave={(e) => {
          // Ignore drag events between children elements of the same card.
          if (e.relatedTarget instanceof Node &&
              e.currentTarget.contains(e.relatedTarget)) {
            return;
          }
          if (draggingTask) {
            if (!taskDropTarget || taskDropTarget.type !== "category") {
              return;
            }
            // Clear the task drop target if leaving the category card.
            if (category.id === taskDropTarget.id) {
              setTaskDropTarget(null);
            }
          }
        }}
        >

        {/* header */}
        <div className={`border-b ${SEPARATER_CATEGORY_BORDER_COLORS[category.color]} pb-2 mt-2`}>
          <div className="px-3 py-2 text-2xl">
            {category.name}
          </div>
        </div>
        <div className={`h-[60px] border-b ${SEPARATER_CATEGORY_BORDER_COLORS[category.color]} flex items-center gap-2`}>
          <HeaderButton
            className="flex items-center gap-1"
            onClick={updateAndAddTask}
            disabled={tasks.length >= MAX_TASKS}
          >
            <Plus className="w-5 h-5"/>
            <span>
              {tasks.length >= MAX_TASKS
                ? "Task Limit Reached"
                : "Add Task"}
            </span>
          </HeaderButton>
          <HeaderButton className="flex items-center gap-2" onClick={() => setIsEditOpen(true)}>
            <SquarePen className="w-5 h-5" />
            <span>Edit</span>
          </HeaderButton>
        </div>
        {/* task drop area*/}
        <div className="p-4 flex flex-col">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              category={category}
              task={task}
              tasks={tasks}
              setTasks={setTasks}
              tasksByCategory={tasksByCategory}
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
              tasksByCategory={tasksByCategory}
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
        <DropArea category={category} handleDrop={handleDrop} setDropTargetCategoryId={setDropTargetCategoryId} />}
    </>
  );
}
