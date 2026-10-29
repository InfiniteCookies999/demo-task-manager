import { SquarePen } from "lucide-react";
import type { Task, TaskDropTarget } from "../types/task";
import { apiFetch } from "../api/apiFetch";
import type { Category } from "../types/category";
import { useAppContext } from "../contexts/AppContext";
import { useState } from "react";
import EditTaskDialog from "./EditTaskDialog";
import ViewTaskDialog from "./ViewTaskDialog";
import TaskCompletedCircle from "./TaskCompletedCircle";
import { useTaskDrag } from "../hooks/useTaskDrag";

type TaskProps = {
  category: Category;
  // Task may be null to simulate a drop area.
  task: Task | null;
  tasks: Task[];
  setTasks: React.Dispatch<React.SetStateAction<Task[]>>;
  tasksByCategory: Record<number, Task[]>;
  setTasksByCategory: React.Dispatch<React.SetStateAction<Record<number, Task[]>>>
  // Dragging
  draggingTask: Task | null;
  setDraggingTask: React.Dispatch<React.SetStateAction<Task | null>>;
  taskDropTarget: TaskDropTarget;
  setTaskDropTarget: React.Dispatch<React.SetStateAction<TaskDropTarget>>;
};

function DropArea(
  {
    className,
    task,
    category,
    handleDrop
  }:
  {
    className: string,
    task: Task | null,
    category: Category,
    handleDrop: (e: React.DragEvent<HTMLDivElement>, targetId: number | null, targetCategoryId: number) => void}
) {
  return (
    <div
      className={`h-[60px] bg-zinc-200 shrink-0 rounded-lg border-b border-zinc-600 ${className}`}
      aria-hidden="true"
      onDragOver={(e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = "move";
      }}
      onDrop={(e) => {
        if (!task) {
          // Fake drop area to drop into an empty category. Handled by useCategoryTaskDrag.ts.
          return;
        }
        e.stopPropagation(); // It is nested inside another div with drop. Preventing from happening twice.
        handleDrop(e, task?.id ?? null, category.id)
      }}
    />
  );
}

export default function TaskCard(
  {
    category,
    task,
    tasks,
    setTasks,
    tasksByCategory,
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
  const [isViewOpen, setIsViewOpen] = useState(false);

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

  const {
    clearDragState,
    handleDragStart,
    handleDragEnter,
    handleDrop
  } = useTaskDrag({
    category,
    task,
    tasks,
    setTasks,
    tasksByCategory,
    setTasksByCategory,
    draggingTask,
    setDraggingTask,
    setTaskDropTarget,
  });

  const isDragTarget = taskDropTarget &&
                       taskDropTarget.type === "task" &&
                       taskDropTarget.id === task?.id &&
                       draggingTask &&
                       draggingTask.id !== task.id;

  const isCategoryDragTarget = taskDropTarget &&
                               taskDropTarget.type === "category" &&
                               taskDropTarget.id === category.id &&
                               (task === null ||
                                  (task.position === tasks.length - 1 &&
                                    draggingTask?.categoryId !== category.id
                                  ));

  return (
    <>
      {/*
        Wrapped in another outer div so that it can more cleanly detect drag enters/leaves from
        the combination of the drop area and the dragged over card.
      */}
      <div
        onDragLeave={(e) => {
          // Ignore drag events between children elements of the same card.
          if (e.relatedTarget instanceof Node &&
              e.currentTarget.contains(e.relatedTarget)) {
            return;
          }
          if (isDragTarget) {
            setTaskDropTarget((current) => {
              // Must be carefull here since it may be drag entering another card while
              // also leaving.
              if (!current) return current;
              const matches = current.type === "task" && current.id == task.id;
              return matches ? null : current;
            });
          }
        }}
        data-task-card
        onDragEnd={clearDragState}
        onDragEnter={(e) => handleDragEnter(e)}
        onDragOver={(e) => {
          e.preventDefault();
          e.dataTransfer.dropEffect = "move";
        }}
        onDrop={(e) => { if (task) handleDrop(e, task.id, category.id)}}
        >
        {(
          (isDragTarget && draggingTask.position > task.position && draggingTask.categoryId === task.categoryId)
        ) &&
          <DropArea className="mb-2" task={task} category={category} handleDrop={handleDrop} />}


        {/* Displaying the actual task */}
        {task &&
        (
          <div
            className={
              `bg-zinc-50 shrink-0 rounded-lg border border-zinc-300 pl-3 flex items-center justify-between text-lg
              mb-2 cursor-pointer
              ${(draggingTask && draggingTask.id === task.id) ? "opacity-30" : "opacity-100"}
            `}
            data-task-id={task.id}
            draggable
            onDragStart={(e) => handleDragStart(e)}
            onClick={() => {
              setIsViewOpen(true);
            }}
            >
            <div className="flex items-start gap-2">
              <button
                type="button"
                title={task.completed ? "Mark Not Complete" : "Mark Complete"}
                aria-label="Mark Complete"
                className="shrink-0 cursor-pointer"
                onClick={(e) => {
                  e.stopPropagation();
                  markCompleted(task.id, task.completed)
                }}
              >
                <TaskCompletedCircle task={task} clickable={false} />
              </button>

              <span className={`min-w-0 break-words ${task.completed ? "text-[#777]" : ""}`}>{task.name}</span>
            </div>

            <div
              className="p-4 rounded-lg cursor-pointer hover:bg-sky-200 shrink-0"
              onClick={(e) => {
                e.stopPropagation();
                setIsEditOpen(true);
              }}
            >
              <SquarePen />
            </div>
          </div>
        )
        }

        {/*
          For now we just place below if its on a different card entirely.
          Ideally this would instead use the onDragOver event to check its
          relative position and determine to place above/below.
        */}
        {(
          ((isDragTarget &&
            (draggingTask.position < task.position || draggingTask.categoryId !== task.categoryId))) ||
          isCategoryDragTarget
         )
           &&
          <DropArea className="my-2" task={task} category={category} handleDrop={handleDrop} />}
      </div>

      {(task && isEditOpen) && (
        <EditTaskDialog
          boardId={category.boardId}
          task={task}
          setTasks={setTasks}
          setTasksByCategory={setTasksByCategory}
          onClose={() => {
            setIsEditOpen(false);
          }}/>
      )}

      {(task && isViewOpen) && (
        <ViewTaskDialog
          task={task}
          onClose={() => {
            setIsViewOpen(false);
          }}
        />
      )}
    </>
  );
}
