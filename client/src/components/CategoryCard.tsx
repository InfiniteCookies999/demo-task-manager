import { useState } from "react";
import type { Category } from "../types/category";
import { CATEGORY_COLORS, SEPARATER_CATEGORY_BORDER_COLORS } from "../types/category";
import { Plus, SquarePen } from "lucide-react";
import EditCategoryDialog from "./EditCategoryDialog";
import type { Task, TaskDropTarget } from "../types/task";
import { useAppContext } from "../contexts/AppContext";
import { apiFetch } from "../api/apiFetch";
import TaskCard from "./TaskCard";
import createDragClone from "../utils/createDragClone";
import { localReorderTasksInCategory } from "../utils/taskReordering";

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
  // ----------------------------------

  function clearDragState() {
    setDraggingCategory(null);
    setDropTargetCategoryId(null);
  }

  function clearTaskDragState() {
    setDraggingTask(null);
    setTaskDropTarget(null);
  }

  function handleDragStart(e: React.DragEvent<HTMLDivElement>) {
    // Prevent dragging when dialogs are open.
    if (document.querySelector("dialog:modal")) {
      e.preventDefault();
      return;
    }

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
          categoryIds: categories.map(({ id }) => id)
        })
      });
    } catch (err) {
      handleError(err);
    }
  }

  async function updateTaskOrder(targetCategoryId: number, tasks: Task[], movedTaskId: number | null) {
    try {
      await apiFetch(`/api/boards/${category.boardId}/tasks/reorder`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          targetCategoryId,
          taskIds: tasks.map(({ id }) => id),
          movedTaskId
        })
      });
    } catch (err) {
      handleError(err);
    }
  }

  function handleTaskDropInEmptyCategory(
    sourceId: number,
    sourceCategoryId: number, targetCategoryId: number
  ) {
    const sourceTasks = tasksByCategory[sourceCategoryId];

    const sourceIndex = sourceTasks.findIndex((task) => task.id === sourceId);
    if (sourceIndex === -1) {
      return;
    }

    const updated = localReorderTasksInCategory(
      tasksByCategory,
      sourceId,
      sourceCategoryId,
      targetCategoryId,
      sourceIndex,
      0
    );

    setTasksByCategory(updated);

    const updatedTargetTasks = updated[targetCategoryId];
    updateTaskOrder(targetCategoryId, updatedTargetTasks, sourceId);
  }

  function handleDrop(
    e: React.DragEvent<HTMLDivElement>,
    targetId: number
  ) {
    e.preventDefault();

    if (draggingTask) {
      if (!taskDropTarget || taskDropTarget.type !== "category") {
        return;
      }
      // This really should not ever happen.
      if (targetId !== taskDropTarget.id) {
        clearTaskDragState();
        return;
      }

      const data = JSON.parse(e.dataTransfer.getData("text/plain"));

      const sourceId: number = data.taskId;
      const sourceCategoryId: number = data.categoryId;

      if (sourceId === targetId) {
        clearTaskDragState();
        return;
      }

      handleTaskDropInEmptyCategory(sourceId, sourceCategoryId, targetId);
      clearTaskDragState();
      return;
    }

    if (!draggingCategory) {
      return;
    }

    const sourceId = Number(e.dataTransfer.getData("text/plain"));

    if (sourceId === targetId) {
      clearDragState();
      return;
    }

    const sourceIndex = categories.findIndex((category) => category.id === sourceId);
    const targetIndex = categories.findIndex((category) => category.id === targetId);

    if (sourceIndex === -1 || targetIndex === -1) {
      clearDragState();
      return;
    }

    const reordered = [...categories];
    const [moved] = reordered.splice(sourceIndex, 1);
    reordered.splice(targetIndex, 0, moved);

    const updated = reordered.map((category, index) => ({
      ...category,
      position: index,
    }));

    // Optimistically update. This could result in order mismatch in the case of multiple
    // drags but waiting on the server response in this case could lead to a poor experience.
    setCategories(updated);
    updateCategoryOrder(updated);
    clearDragState();
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
        <DropArea category={category} handleDrop={handleDrop} setDropTargetCategoryId={setDropTargetCategoryId} />}

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
        onDragEnd={clearDragState}
        onDragEnter={(e) => handleDragEnter(e)}
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
          <HeaderButton className="flex items-center gap-1" onClick={updateAndAddTask}>
            <Plus className="w-5 h-5"/>
            <span>Add Task</span>
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
