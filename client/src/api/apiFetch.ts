
export async function apiFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  let response: Response;

  try {
    response = await fetch(input, init);
  } catch {
    throw new Error("Unable to connect to the server. Please check your connection and try again.");
  }

  if (!response.ok) {

    let message = `Request failed (${response.status}) at (${input})`;

    try {
      const body = await response.json();
      if (typeof body.message === "string") {
        message += ": " + body.message;
      }
    } catch {
      // Not JSON
    }

    throw new Error(message);
  }

  return response;
}
