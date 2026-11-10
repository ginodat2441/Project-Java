"use strict";
(() => {
  function attach(root) {
    if (!root) return;
    for (const input of root.querySelectorAll(
      "input[type=password]:not([data-visibility-bound])",
    )) {
      input.dataset.visibilityBound = "true";
      const wrapper = document.createElement("span");
      wrapper.className = "password-input";
      input.before(wrapper);
      wrapper.append(input);
      const toggle = document.createElement("button");
      toggle.type = "button";
      toggle.className = "password-toggle";
      toggle.textContent = "Hiện";
      toggle.setAttribute("aria-label", "Hiện mật khẩu");
      toggle.setAttribute("aria-pressed", "false");
      toggle.onclick = () => {
        const show = input.type === "password";
        input.type = show ? "text" : "password";
        toggle.textContent = show ? "Ẩn" : "Hiện";
        toggle.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
        toggle.setAttribute("aria-pressed", String(show));
      };
      wrapper.append(toggle);
    }
  }
  attach(document);
  const observer = new MutationObserver(() => attach(document));
  observer.observe(document.body, { childList: true, subtree: true });
  window.addEventListener("pagehide", () => observer.disconnect(), { once: true });
})();
