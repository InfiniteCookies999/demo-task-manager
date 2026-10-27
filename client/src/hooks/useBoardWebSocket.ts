import { Client } from "@stomp/stompjs";
import { useEffect } from "react";
import type { BoardEvent } from "../types/event";


export function useBoardWebSocket(boardId: number, onEvent: (event: BoardEvent) => void) {

  useEffect(() => {

    const protocol =
      window.location.protocol === "https:" ? "wss:" : "ws:";

    const url = `${protocol}//${window.location.host}/ws`

    const client = new Client({
      brokerURL: url,
      reconnectDelay: 5000, // If connection is lost try again after 5 seconds.

      onConnect: () => {
        console.log("Connected to websocket");

        client.subscribe(`/topic/board/${boardId}`, (message) => {
          const event: BoardEvent = JSON.parse(message.body);
          onEvent(event);
        });
      },

      onStompError: (frame) => {
        // TODO (maddie): handle
        console.error("STOMP error:", frame.headers["message"]);
      },

      onWebSocketError: (error) => {
        // TODO (maddie): handle.
        console.error("WebSocket error:", error);
      }

    });

    console.log("attempting web socket connection");
    client.activate();

    return () => {
      client.deactivate();
    };
  }, [boardId]);
}