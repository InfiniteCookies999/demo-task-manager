
export default function createDragClone(cloneable: HTMLElement) {
  // Create a clone of the element so we can modify the drag image.
  const clone = cloneable.cloneNode(true) as HTMLElement;

  clone.style.position = "absolute";
  // make sure it does not appear on the page since we are adding it to the DOM and only want it visible
  // through the drag image.
  clone.style.top = "-9999px";
  clone.style.left = "-9999px";
  clone.style.width = `${cloneable.offsetWidth}px`;
  clone.style.opacity = "0.9";
  clone.style.pointerEvents = "none";
  // clone.style.boxShadow = "0 25px 50px rgba(0, 0, 0, 0.5), 0 10px 20px rgba(0, 0, 0, 0.15)";
  clone.style.transform = "rotate(2deg)";

  return clone;
}