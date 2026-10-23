import { Circle, CircleCheck } from "lucide-react";
import type { Task } from "../types/task";

type TaskCompletedCircleProps = {
  task: Task;
  clickable: boolean;
};

export default function TaskCompletedCircle({ task, clickable }: TaskCompletedCircleProps) {
  return (
    <>
      {
        task.completed
          ? <CircleCheck className={
            `w-5.5 h-5.5 shrink-0 translate-y-0.5 fill-emerald-300 ${clickable ? "cursor-pointer" : ""}`
          }/>
          : <Circle className={`w-5.5 h-5.5 shrink-0 translate-y-0.5 ${clickable ? "cursor-pointer" : ""}`}/>
      }
    </>
  );
}
