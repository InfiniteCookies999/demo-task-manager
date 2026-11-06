import { useParams } from "react-router-dom";
import Board from "./Board";

type BoardRouteProps = {
  error: string;
};

export default function BoardRoute({ error }: BoardRouteProps) {
  const { boardId } = useParams<{ boardId: string }>();

  if (!boardId || !Number.isInteger(Number(boardId)) || Number(boardId) <= 0) {
    return <p>Invalid board ID.</p>;
  }

  return <Board boardId={Number(boardId)} error={error} />
}
