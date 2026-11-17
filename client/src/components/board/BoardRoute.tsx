import { useParams } from "react-router-dom";
import Board from "./Board";


export default function BoardRoute() {
  const { boardId } = useParams<{ boardId: string }>();

  if (!boardId || !Number.isInteger(Number(boardId)) || Number(boardId) <= 0) {
    return <p>Invalid board ID.</p>;
  }

  // Making sure to set the key so we get a full remount of the board to prevent stale state.
  return <Board key={Number(boardId)} boardId={Number(boardId)} />
}
