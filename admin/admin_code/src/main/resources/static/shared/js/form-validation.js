(() => {
  const localized = new WeakSet();

  document.addEventListener("invalid", (event) => {
    const field = event.target;
    if (field.validity.customError) return;

    const labels = [...(field.labels || [])].map((label) => {
      const copy = label.cloneNode(true);
      copy.querySelectorAll("input, select, textarea, button").forEach((control) => control.remove());
      return copy.textContent.trim();
    });
    const name = labels.find(Boolean) || field.getAttribute("aria-label") ||
      field.getAttribute("placeholder") || "Trường này";
    const validity = field.validity;
    let reason = "có giá trị không hợp lệ.";
    if (validity.valueMissing) reason = "là trường bắt buộc.";
    else if (validity.typeMismatch || validity.patternMismatch) reason = "không đúng định dạng.";
    else if (validity.tooShort) reason = "phải có ít nhất " + field.minLength + " ký tự.";
    else if (validity.tooLong) reason = "chỉ được có tối đa " + field.maxLength + " ký tự.";
    else if (validity.rangeUnderflow) reason = "phải từ " + field.min + " trở lên.";
    else if (validity.rangeOverflow) reason = "không được vượt quá " + field.max + ".";
    else if (validity.stepMismatch || validity.badInput) reason = "phải nhập giá trị đúng định dạng.";

    field.setCustomValidity(name + " " + reason);
    localized.add(field);
  }, true);

  function clearLocalizedError(event) {
    const field = event.target;
    if (localized.delete(field)) field.setCustomValidity("");
  }

  document.addEventListener("input", clearLocalizedError);
  document.addEventListener("change", clearLocalizedError);
  document.addEventListener("reset", (event) => {
    for (const field of event.target.elements) {
      if (localized.delete(field)) field.setCustomValidity("");
    }
  });
})();
