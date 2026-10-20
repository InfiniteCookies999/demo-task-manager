import { Circle, CircleCheck, SquarePen } from "lucide-react";
import type { Task, TaskDropTarget } from "../types/task";
import { apiFetch } from "../api/apiFetch";
import type { Category } from "../types/category";
import { useAppContext } from "../api/AppContext";
import { useState } from "react";
import EditTaskDialog from "./EditTaskDialog";
import createDragClone from "../utils/createDragClone";

type TaskProps = {
  category: Category;
  // Task may be null to simulate a drop area.
  task: Task | null;
  tasks: Task[];
  setTasks: React.Dispatch<React.SetStateAction<Task[]>>;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>
  // Dragging
  draggingTask: Task | null;
  setDraggingTask: React.Dispatch<React.SetStateAction<Task | null>>;
  taskDropTarget: TaskDropTarget;
  setTaskDropTarget: React.Dispatch<React.SetStateAction<TaskDropTarget>>;
};

function DropArea(
  {
    task,
    category,
    handleDrop
  }:
  {
    task: Task | null,
    category: Category,
    handleDrop: (e: React.DragEvent<HTMLDivElement>, targetId: number | null, targetCategoryId: number) => void}
) {
  return (
    <div
      className="h-[60px] bg-zinc-200 shrink-0 rounded-lg border-b border-zinc-600"
      aria-hidden="true"
      onDragOver={(e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
      }}
      onDrop={(e) => handleDrop(e, task?.id ?? null, category.id)}
    />
  );
}

export default function TaskCard(
  {
    category,
    task,
    tasks,
    setTasks,
    setTasksByCategory,
    // Dragging
    draggingTask,
    setDraggingTask,
    taskDropTarget,
    setTaskDropTarget
  }:
  TaskProps
) {

  const { handleError } = useAppContext();

  const [isEditOpen, setIsEditOpen] = useState(false);

  async function markCompleted(taskId: number, completed: boolean) {
    try {
      const newCompleted = !completed;
      await apiFetch(`/api/boards/${category.boardId}/tasks/${taskId}`, {
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

  function handleDragStart(e: React.DragEvent<HTMLDivElement>) {
    e.stopPropagation(); // Prevent the parent category from dragging.

    if (!task) {
      return;
    }

    if (isEditOpen) {
      // Note: for some reasion when the edit dialog is open its still allowing drag.
      e.preventDefault();
      return;
    }

    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", JSON.stringify({
      taskId: task.id,
      categoryId: category.id
    }));

    const elm = e.currentTarget.closest("[data-task-id]") as HTMLElement | null;

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

    setDraggingTask(task);
  }

  function handleDragEnter(e: React.DragEvent<HTMLDivElement>) {
    // Ignore drag events between children elements of the same card.
    if (e.relatedTarget instanceof Node &&
        e.currentTarget.contains(e.relatedTarget)) {
      return;
    }

    if (!draggingTask) {
      return;
    }
    if (!task) {
      return;
    }

    setTaskDropTarget({ type: "task", id: task.id });
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>, targetId: number | null, targetCategoryId: number) {
    e.preventDefault();

    const data = JSON.parse(e.dataTransfer.getData("text/plain"));

    const sourceId: number = data.taskId;
    const sourceCategoryId: number = data.categoryId;


    if (sourceId === targetId) {
      console.log("source and target match returning");
      return;
    }

    if (targetId === null) {
      // TODO (maddie): this is the case where we drop into an empty category.
      console.log("empty category case");
      return;
    }

    if (sourceCategoryId === targetCategoryId) {
      console.log("same category case");
      return;
    }
    console.log("drop into different category case");
  }

  const isDragTarget = taskDropTarget &&
                       taskDropTarget.type === "task" &&
                       taskDropTarget.id === task?.id &&
                       draggingTask &&
                       draggingTask.id !== task.id;

  const isCategoryDragTarget = taskDropTarget &&
                               taskDropTarget.type === "category" &&
                               taskDropTarget.id === category.id;

  return (
    <>
      {(
        (isDragTarget && draggingTask.position > task.position && draggingTask.categoryId === task.categoryId) ||
        isCategoryDragTarget
       ) &&
        <DropArea task={task} category={category} handleDrop={handleDrop} />}

      {task &&
      (
        <div
          className={
            `bg-zinc-50 shrink-0 rounded-lg border border-zinc-300 p-4 flex justify-between text-lg
            ${(draggingTask && draggingTask.id === task.id) ? "opacity-30" : "opacity-100"}
          `}
          data-task-id={task.id}
          draggable
          onDragStart={(e) => handleDragStart(e)}
          onDragEnd={() => {
            setDraggingTask(null);
            setTaskDropTarget(null);
          }}
          onDragEnter={(e) => handleDragEnter(e)}
          onDragOver={(e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = "move";
          }}
          onDrop={(e) => handleDrop(e, task.id, category.id)}
          >
          <div className="flex items-start gap-2">
            <button
              type="button"
              title={task.completed ? "Mark Not Complete" : "Mark Complete"}
              aria-label="Mark Complete"
              className="shrink-0 cursor-pointer"
              onClick={() => markCompleted(task.id, task.completed)}
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
              setIsEditOpen(true);
            }}
            >
          </SquarePen>
        </div>
      )
      }

      {(task && isEditOpen) && (
        <EditTaskDialog
          boardId={category.boardId}
          task={task}
          setTasks={setTasks}
          onClose={() => {
            setIsEditOpen(false);
          }}/>
      )}

      {/*
        For now we just place below if its on a different card entirely.
        Ideally this would instead use the onDragOver event to check its
        relative position and determine to place above/below.
      */}
      {(isDragTarget && (draggingTask.position < task.position || draggingTask.categoryId !== task.categoryId)) &&
        <DropArea task={task} category={category} handleDrop={handleDrop} />}
    </>
  );
}