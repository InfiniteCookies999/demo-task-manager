import { useParams } from "react-router-dom";
import type { User } from "../types/user";
import Board from "./Board";

type BoardRouteProps = {
  user: User;
  error: string;
};

export default function BoardRoute({ user, error }: BoardRouteProps) {
  const { boardId } = useParams<{ boardId: string }>();

  if (!boardId || !Number.isInteger(Number(boardId)) || Number(boardId) <= 0) {
    return <p>Invalid board ID.</p>;
  }

  return <Board user={user} boardId={Number(boardId)} error={error} />
}
