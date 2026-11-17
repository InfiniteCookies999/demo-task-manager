import { ApiError, ApiErrorCode, apiErrorCodes } from "./ApiError";

export async function apiFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  let response: Response;

  try {
    response = await fetch(input, init);
  } catch {
    throw new Error("Unable to connect to the server. Please check your connection and try again.");
  }

  if (!response.ok) {

    let message = `Request failed (${response.status}) at (${input})`;
    let code: ApiErrorCode = ApiErrorCode.UNKNOWN;

    try {
      const body = await response.json();
      if (typeof body.message === "string") {
        message += ": " + body.message;
      }
      if (typeof body.code === "string" && apiErrorCodes.has(body.code)) {
        code = body.code as ApiErrorCode;
      }
    } catch {
      // Not JSON
    }

    throw new ApiError(message, response.status, code);
  }

  return response;
}
