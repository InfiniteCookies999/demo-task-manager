import { Client } from "@stomp/stompjs";
import { useEffect } from "react";


export function useBoardWebSocket(boardId: number) {

  useEffect(() => {

    const protocol =
      //window.location.protocol === "https:" ? "wss:" : "ws:";
    "http:";

    const url = `${protocol}//${window.location.host}/ws`

    console.log("attempting to connect to websocket: " + url);

    const client = new Client({
      brokerURL: url,
      reconnectDelay: 5000, // If connection is lost try again after 5 seconds.

      onConnect: () => {
        console.log("Connected to websocket");

        client.subscribe(`/topic/board/${boardId}`, (message) => {
          const event = JSON.parse(message.body);
          console.log("event recieved");
          console.log(event);
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

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [boardId]);
}