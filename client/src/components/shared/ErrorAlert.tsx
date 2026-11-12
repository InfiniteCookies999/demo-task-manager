
export default function ErrorAlert({ error }: { error: string }) {
  return (
    <>
      {error && (
        <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
          <p>{error}</p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-2 cursor-pointer underline"
          >
            Retry
          </button>
        </div>
      )}
    </>
  );
}
