/**
 * Template Name: eStore
 * Template URL: https://bootstrapmade.com/estore-bootstrap-ecommerce-template/
 * Author: BootstrapMade.com
 * License: https://bootstrapmade.com/license/
 */
(function () {
  "use strict";

  const mobileNavToggle = document.querySelector(".mobile-nav-toggle");
  const closeMobileNav = function () {
    document.body.classList.remove("mobile-nav-active");
    mobileNavToggle?.classList.remove("bi-x");
    mobileNavToggle?.classList.add("bi-list");
    mobileNavToggle?.setAttribute("aria-expanded", "false");
  };

  mobileNavToggle?.addEventListener("click", function () {
    const isOpen = document.body.classList.toggle("mobile-nav-active");
    mobileNavToggle.classList.toggle("bi-list", !isOpen);
    mobileNavToggle.classList.toggle("bi-x", isOpen);
    mobileNavToggle.setAttribute("aria-expanded", String(isOpen));
  });

  document.querySelectorAll("#navmenu a").forEach(function (link) {
    link.addEventListener("click", closeMobileNav);
  });

  const preloader = document.querySelector("#preloader");
  if (preloader) window.addEventListener("load", function () { preloader.remove(); });

  const scrollTop = document.querySelector(".scroll-top");
  if (scrollTop) {
    const updateScrollTop = function () {
      scrollTop.classList.toggle("active", window.scrollY > 100);
    };
    scrollTop.addEventListener("click", function (event) {
      event.preventDefault();
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
    window.addEventListener("load", updateScrollTop);
    document.addEventListener("scroll", updateScrollTop);
  }
})();