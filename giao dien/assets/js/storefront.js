(function () {
  "use strict";

  const apiBase = (window.CLOTHING_SHOP_API_BASE || "http://localhost:8080/api").replace(/\/+$/, "");
  const tokenKey = "clothingShopToken";
  const userKey = "clothingShopUser";
  const orderKey = "clothingShopLastOrder";
  const money = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });

  const stylesheet = document.createElement("link");
  stylesheet.rel = "stylesheet";
  stylesheet.href = "assets/css/storefront.css";
  document.head.appendChild(stylesheet);

  const translations = {
    "Need help? Call us:": "Cần hỗ trợ? Gọi cho chúng tôi:",
    "Free shipping on orders over $50": "Thanh toán khi nhận hàng",
    "30 days money back guarantee.": "Phí giao hàng hiển thị khi thanh toán",
    "20% off on your first order": "Hỗ trợ tư vấn chọn size",
    "My Profile": "Tài khoản của tôi",
    "My Orders": "Đơn hàng của tôi",
    "My Wishlist": "Sản phẩm yêu thích",
    "Settings": "Cài đặt",
    "Sign In": "Đăng nhập",
    "Register": "Đăng ký",
    "Access account & manage orders": "Quản lý tài khoản và đơn hàng",
    "Welcome to": "Chào mừng đến",
    "Search for products": "Tìm kiếm sản phẩm",
    "Search foree products": "Tìm kiếm sản phẩm",
    "Home": "Trang chủ",
    "Shop": "Cửa hàng",
    "Products": "Sản phẩm",
    "Product": "Sản phẩm",
    "Category": "Danh mục",
    "Categories": "Danh mục",
    "Product Details": "Chi tiết sản phẩm",
    "Checkout": "Thanh toán",
    "Cart": "Giỏ hàng",
    "Contact": "Liên hệ",
    "Contact Us": "Liên hệ với chúng tôi",
    "About": "Giới thiệu",
    "About Us": "Về chúng tôi",
    "Support": "Hỗ trợ",
    "Help Center": "Trung tâm trợ giúp",
    "Order Status": "Theo dõi đơn hàng",
    "Shipping Info": "Thông tin giao hàng",
    "Returns & Exchanges": "Đổi trả hàng",
    "Hướng dẫn chọn size": "Hướng dẫn chọn size",
    "Privacy Policy": "Chính sách bảo mật",
    "Terms of Service": "Điều khoản dịch vụ",
    "Featured": "Nổi bật",
    "Featured Products": "Sản phẩm nổi bật",
    "Sale Items": "Sản phẩm",
    "New Arrivals": "Sản phẩm mới",
    "Bestsellers": "Bán chạy",
    "Best Sellers": "Bán chạy",
    "Women's Clothing": "Thời trang nữ",
    "Men's Clothing": "Thời trang nam",
    "Accessories": "Phụ kiện",
    "Footwear": "Giày dép",
    "Clothing": "Quần áo",
    "Collection": "Bộ sưu tập",
    "Women": "Nữ",
    "Men": "Nam",
    "Kids": "Trẻ em",
    "Kids'": "Trẻ em",
    "Women's": "Nữ",
    "Men's": "Nam",
    "WOMEN": "NỮ",
    "Men's Wear": "Thời trang nam",
    "Women's Wear": "Thời trang nữ",
    "Kids Collection": "Bộ sưu tập trẻ em",
    "Sportswear": "Đồ thể thao",
    "Shirts & Tops": "Áo sơ mi & áo kiểu",
    "T-shirts & Tops": "Áo thun & áo kiểu",
    "Coats & Outerwear": "Áo khoác",
    "Jackets & Coats": "Áo khoác",
    "Dresses": "Đầm",
    "Sweatshirts": "Áo nỉ",
    "Hoodies": "Áo hoodie",
    "Underwear": "Đồ mặc trong",
    "Swimwear": "Đồ bơi",
    "Shoes": "Giày",
    "Boots": "Bốt",
    "Sandals": "Dép sandal",
    "Slippers": "Dép",
    "Sneakers": "Giày thể thao",
    "Loafers": "Giày lười",
    "Heels": "Giày cao gót",
    "Oxfords": "Giày tây",
    "Accessories": "Phụ kiện",
    "Belts": "Thắt lưng",
    "Handbags": "Túi xách",
    "Backpacks": "Ba lô",
    "Hats & Caps": "Mũ",
    "Socks": "Tất",
    "Scarves": "Khăn choàng",
    "Gloves": "Găng tay",
    "Jewelry": "Trang sức",
    "Watches": "Đồng hồ",
    "Sunglasses": "Kính mát",
    "View all": "Xem tất cả",
    "View All": "Xem tất cả",
    "Shop now": "Mua ngay",
    "View Product": "Xem sản phẩm",
    "Join Our Newsletter": "Đăng ký nhận tin",
    "Subscribe": "Đăng ký",
    "Your email address": "Địa chỉ email của bạn",
    "Your subscription request has been sent. Thank you!": "Đăng ký nhận tin thành công. Cảm ơn bạn!",
    "Shop on the go with our mobile app": "Mua sắm thuận tiện mọi lúc",
    "Download Our App": "Tải ứng dụng",
    "Company": "Công ty",
    "Careers": "Tuyển dụng",
    "Press": "Báo chí",
    "Affiliates": "Đối tác",
    "Responsibility": "Trách nhiệm xã hội",
    "Investors": "Nhà đầu tư",
    "Cookies Settings": "Cài đặt cookie",
    ". All Rights Reserved.": ". Đã đăng ký bản quyền.",
    "Sale": "Sản phẩm",
    "About Us": "Về chúng tôi",
    "Dropdown": "Danh mục",
    "Dropdown 1": "Danh mục 1",
    "Dropdown 2": "Danh mục 2",
    "Dropdown 3": "Danh mục 3",
    "Dropdown 4": "Danh mục 4",
    "Deep Dropdown": "Danh mục con",
    "Deep Dropdown 1": "Danh mục con 1",
    "Deep Dropdown 2": "Danh mục con 2",
    "Deep Dropdown 3": "Danh mục con 3",
    "Deep Dropdown 4": "Danh mục con 4",
    "Deep Dropdown 5": "Danh mục con 5",
    "Megamenu 1": "Thời trang",
    "Megamenu 2": "Bộ sưu tập",
    "My account": "Tài khoản",
    "Loading": "Đang tải",
    "Next": "Tiếp",
    "Previous": "Trước",
    "Read More": "Đọc thêm",
    "Continue Shopping": "Tiếp tục mua sắm",
    "Proceed to Checkout": "Tiến hành thanh toán",
    "Add to Cart": "Thêm vào giỏ",
    "Buy Now": "Mua ngay",
    "Quantity": "Số lượng",
    "Size": "Kích cỡ",
    "Color": "Màu sắc",
    "Description": "Mô tả",
    "Specifications": "Thông tin sản phẩm",
    "Reviews": "Đánh giá",
    "Write a Review": "Viết đánh giá",
    "Your Rating": "Đánh giá của bạn",
    "Price": "Giá",
    "Availability": "Tình trạng",
    "In Stock": "Còn hàng",
    "Out of Stock": "Hết hàng",
    "Order Summary": "Tóm tắt đơn hàng",
    "Subtotal": "Tạm tính",
    "Shipping": "Phí giao hàng",
    "Total": "Tổng cộng",
    "Full Name": "Họ và tên",
    "Email": "Email",
    "Password": "Mật khẩu",
    "Phone": "Số điện thoại",
    "Address": "Địa chỉ",
    "Country": "Quốc gia",
    "City": "Tỉnh / Thành phố",
    "State": "Quận / Huyện",
    "Postal Code": "Mã bưu chính",
    "First Name": "Tên",
    "Last Name": "Họ",
    "Order Confirmation": "Xác nhận đơn hàng",
    "Thank you for your order!": "Cảm ơn bạn đã đặt hàng!",
    "Your order has been placed successfully.": "Đơn hàng của bạn đã được tiếp nhận.",
    "Order Number": "Mã đơn hàng",
    "Payment Method": "Phương thức thanh toán",
    "Cash on Delivery": "Thanh toán khi nhận hàng",
    "COD": "Thanh toán khi nhận hàng",
    "eStore": "eStore"
  };
  Object.assign(translations, {
    "Subscribe to get special offers, free giveaways, and once-in-a-lifetime deals.": "Nhận cập nhật về sản phẩm và thông tin mới từ cửa hàng.",
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam in nibh vehicula, facilisis magna ut, consectetur lorem.": "Cửa hàng mang đến những lựa chọn thời trang thiết yếu, chất lượng và dễ phối cho phong cách hàng ngày.",
    "123 Fashion Street, New York, NY 10001": "Thông tin địa chỉ cửa hàng sẽ được cập nhật.",
    "hello@example.com": "Thông tin email hỗ trợ sẽ được cập nhật.",
    "We Accept:": "Phương thức thanh toán:",
    "All Rights Reserved.": "Đã đăng ký bản quyền.",
    "Copyright": "Bản quyền",
    "Designed by": "Thiết kế bởi",
    "Premium Headphones": "Áo sơ mi basic",
    "Smart Watch": "Áo khoác thường ngày",
    "Wireless Earbuds": "Áo thun cotton",
    "Bluetooth Speaker": "Quần kaki",
    "Fitness Tracker": "Quần jean",
    "Wireless Charger": "Áo len cổ tròn",
    "Smart Bulb Set": "Bộ đồ mặc nhà",
    "Portable Power Bank": "Áo khoác nhẹ",
    "Wireless Keyboard": "Áo polo",
    "Gaming Mouse": "Quần jogger",
    "Desk Lamp": "Chân váy",
    "USB-C Hub": "Túi đeo",
    "Electronics": "Thời trang",
    "Smartphones": "Áo thun",
    "Laptops": "Quần",
    "Audio Devices": "Phụ kiện",
    "Smart Home": "Đồ mặc nhà",
    "Home & Living": "Phong cách sống",
    "Furniture": "Thời trang",
    "Decor": "Phụ kiện",
    "Kitchen": "Đồ mặc nhà",
    "Bedding": "Đồ mặc nhà",
    "Lighting": "Phụ kiện",
    "Beauty": "Phong cách",
    "Skincare": "Chăm sóc vải",
    "Makeup": "Phụ kiện",
    "Fragrances": "Phụ kiện",
    "Haircare": "Chăm sóc trang phục",
    "Personal Care": "Chăm sóc cá nhân",
    "12-month warranty": "Chất liệu được mô tả trong thông tin sản phẩm",
    "2-year warranty": "Hỗ trợ đổi trả theo chính sách cửa hàng",
    "Free shipping on orders over $50": "Thanh toán khi nhận hàng",
    "Sale": "Sản phẩm",
    "New": "Mới",
    "Featured": "Nổi bật",
    "New Arrivals": "Sản phẩm mới",
    "New Arrival": "Sản phẩm mới",
    "Shop by Category": "Mua sắm theo danh mục",
    "Shop by category": "Mua sắm theo danh mục",
    "Explore our collection": "Khám phá bộ sưu tập",
    "Our Products": "Sản phẩm của chúng tôi",
    "All Products": "Tất cả sản phẩm",
    "Trending": "Xu hướng",
    "Trending Now": "Xu hướng hiện nay",
    "Best Sellers": "Sản phẩm bán chạy",
    "Featured Collection": "Bộ sưu tập nổi bật",
    "Related Products": "Sản phẩm liên quan",
    "Recently Viewed": "Sản phẩm đã xem",
    "Customer Reviews": "Đánh giá của khách hàng",
    "Product Overview": "Tổng quan sản phẩm",
    "Key Features": "Đặc điểm nổi bật",
    "What's in the Box": "Thông tin sản phẩm",
    "Technical Specifications": "Thông số sản phẩm",
    "Features": "Đặc điểm",
    "Colors:": "Màu sắc:",
    "Size:": "Kích cỡ:",
    "Quantity:": "Số lượng:",
    "Out of Stock": "Hết hàng",
    "In Stock": "Còn hàng",
    "We Accept": "Phương thức thanh toán",
    "Sort by": "Sắp xếp theo",
    "Filter": "Bộ lọc",
    "Apply Filters": "Áp dụng bộ lọc",
    "Clear Filters": "Xóa bộ lọc",
    "Showing": "Đang hiển thị",
    "of": "trên",
    "Results": "kết quả",
    "No products found": "Không tìm thấy sản phẩm",
    "Page Not Found": "Không tìm thấy trang",
    "Oops! Page Not Found": "Rất tiếc! Không tìm thấy trang",
    "Go to Home": "Về trang chủ",
    "Frequently Asked Questions": "Câu hỏi thường gặp",
    "How can we help you?": "Chúng tôi có thể hỗ trợ gì cho bạn?",
    "Get in Touch": "Liên hệ với chúng tôi",
    "Send Message": "Gửi tin nhắn",
    "Your Message": "Nội dung tin nhắn",
    "Subject": "Chủ đề",
    "Name": "Họ tên",
    "Message": "Tin nhắn",
    "Shipping & Returns": "Giao hàng và đổi trả",
    "Return Policy": "Chính sách đổi trả",
    "Payment Methods": "Phương thức thanh toán",
    "Privacy": "Quyền riêng tư",
    "Terms": "Điều khoản",
    "Latest Articles": "Bài viết mới nhất",
    "Read More": "Đọc thêm",
    "Continue Reading": "Đọc tiếp",
    "Related Articles": "Bài viết liên quan",
    "Share This Article": "Chia sẻ bài viết",
    "Back to Blog": "Quay lại bài viết",
    "Start Shopping": "Bắt đầu mua sắm",
    "Track Your Order": "Theo dõi đơn hàng",
    "Order Details": "Chi tiết đơn hàng",
    "Billing Details": "Thông tin nhận hàng",
    "Shipping Address": "Địa chỉ giao hàng",
    "Place Order": "Đặt hàng",
    "Continue to Checkout": "Tiếp tục thanh toán",
    "Your Cart": "Giỏ hàng của bạn",
    "Your cart is empty": "Giỏ hàng của bạn đang trống",
    "Remove": "Xóa",
    "Update Cart": "Cập nhật giỏ hàng",
    "Order Total": "Tổng đơn hàng",
    "Estimated delivery": "Thời gian giao hàng dự kiến",
    "Frequently bought together": "Sản phẩm thường được chọn",
    "Email address": "Địa chỉ email",
    "Confirm Password": "Xác nhận mật khẩu",
    "Forgot Password?": "Quên mật khẩu?",
    "Don't have an account?": "Chưa có tài khoản?",
    "Already have an account?": "Đã có tài khoản?",
    "Create Account": "Tạo tài khoản",
    "Create an Account": "Tạo tài khoản",
    "Welcome Back": "Chào mừng bạn quay lại",
    "Account Details": "Thông tin tài khoản",
    "Personal Information": "Thông tin cá nhân",
    "Save Changes": "Lưu thay đổi",
    "Default Address": "Địa chỉ mặc định",
    "Delivery Information": "Thông tin giao nhận",
    "Order History": "Lịch sử đơn hàng",
    "Recent Orders": "Đơn hàng gần đây",
    "View Details": "Xem chi tiết",
    "Cancel Order": "Hủy đơn hàng",
    "Status": "Trạng thái",
    "Date": "Ngày",
    "Thank you for your order!": "Cảm ơn bạn đã đặt hàng!",
    "Your order has been placed successfully.": "Đơn hàng của bạn đã được tiếp nhận.",
    "Cash on Delivery": "Thanh toán khi nhận hàng",
    "Our Story": "Câu chuyện của chúng tôi",
    "Our Mission": "Sứ mệnh",
    "Our Values": "Giá trị của chúng tôi",
    "Meet Our Team": "Đội ngũ của chúng tôi",
    "Learn More": "Tìm hiểu thêm",
    "Explore More": "Khám phá thêm",
    "Thank you": "Cảm ơn bạn",
    "Oops!": "Rất tiếc!"
  });

  function escapeHtml(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, function (char) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char];
    });
  }

  function localizeStaticText() {
    document.documentElement.lang = "vi";
    const pageTitles = {
      "index.html": "Trang chủ",
      "category.html": "Danh mục sản phẩm",
      "search-results.html": "Kết quả tìm kiếm",
      "product-details.html": "Chi tiết sản phẩm",
      "cart.html": "Giỏ hàng",
      "checkout.html": "Thanh toán",
      "login-register.html": "Đăng nhập / Đăng ký",
      "account.html": "Tài khoản của tôi",
      "order-confirmation.html": "Chi tiết đơn hàng",
      "size-guide.html": "Hướng dẫn chọn size",
      "about.html": "Giới thiệu",
      "contact.html": "Liên hệ",
      "faq.html": "Câu hỏi thường gặp",
      "payment-methods.html": "Phương thức thanh toán",
      "privacy.html": "Chính sách quyền riêng tư",
      "return-policy.html": "Chính sách đổi trả",
      "shiping-info.html": "Thông tin giao hàng",
      "support.html": "Trung tâm hỗ trợ",
      "tos.html": "Điều khoản sử dụng",
      "blog.html": "Cẩm nang mặc đẹp",
      "blog-details.html": "Cẩm nang mặc đẹp",
      "starter-page.html": "Thông tin cửa hàng",
      "404.html": "Không tìm thấy trang"
    };
    const filename = window.location.pathname.split("/").pop().toLowerCase();
    if (pageTitles[filename]) document.title = pageTitles[filename] + " | eStore";

    document.querySelectorAll(".top-bar-item.dropdown").forEach(function (dropdown) {
      const link = dropdown.querySelector(".dropdown-toggle");
      if (!link) return;
      const isCurrency = Boolean(dropdown.querySelector(".bi-currency-dollar"));
      link.innerHTML = isCurrency
        ? '<i class="bi bi-currency-exchange me-2" aria-hidden="true"></i>VND'
        : '<i class="bi bi-translate me-2" aria-hidden="true"></i>Tiếng Việt';
      link.classList.remove("dropdown-toggle");
      link.removeAttribute("data-bs-toggle");
      dropdown.querySelector(".dropdown-menu")?.remove();
    });

    const navigation = document.querySelector("#navmenu > ul");
    if (navigation) {
      const links = [
        ["index.html", "Trang chủ"],
        ["category.html", "Sản phẩm"],
        ["size-guide.html", "Hướng dẫn chọn size"],
        ["faq.html", "Câu hỏi thường gặp"],
        ["contact.html", "Liên hệ"]
      ];
      navigation.innerHTML = links.map(function (entry) {
        const active = filename === entry[0] ? ' class="active"' : "";
        return '<li><a href="' + entry[0] + '"' + active + ">" + entry[1] + "</a></li>";
      }).join("");
      navigation.addEventListener("click", function (event) {
        if (event.target.closest("a") && document.body.classList.contains("mobile-nav-active")) {
          document.body.classList.remove("mobile-nav-active");
          const toggle = document.querySelector(".mobile-nav-toggle");
          toggle?.classList.remove("bi-x");
          toggle?.classList.add("bi-list");
        }
      });
    }

    document.querySelectorAll(".header-action-btn").forEach(function (button) {
      if (button.querySelector(".bi-heart")) button.closest("a")?.remove();
    });

    document.querySelectorAll(".announcement-slider .swiper-slide").forEach(function (slide, index) {
      slide.textContent = [
        "Thanh toán khi nhận hàng",
        "Phí giao hàng hiển thị khi thanh toán",
        "Hỗ trợ tư vấn chọn size"
      ][index % 3];
    });

    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    let node;
    while ((node = walker.nextNode())) {
      const parent = node.parentElement;
      if (!parent || /^(SCRIPT|STYLE|NOSCRIPT|TEXTAREA)$/i.test(parent.tagName)) continue;
      const original = node.nodeValue.trim();
      if (translations[original]) node.nodeValue = node.nodeValue.replace(original, translations[original]);
    }

    document.querySelectorAll("[placeholder], [aria-label], [title]").forEach(function (element) {
      ["placeholder", "aria-label", "title"].forEach(function (attribute) {
        const value = element.getAttribute(attribute);
        if (value && translations[value]) element.setAttribute(attribute, translations[value]);
      });
    });
    document.querySelectorAll('a[href="#"]').forEach(function (link) {
      if (/^(Size Guide|Sale|Bestsellers|New Arrivals)$/.test(link.textContent.trim())) {
        link.setAttribute("href", "category.html");
      }
    });
    document.querySelectorAll('a[href="account.html"]').forEach(function (link) {
      if (link.closest(".account-dropdown")) link.href = "account.html";
    });
    document.querySelectorAll('a[href^="tel:"], a[href^="mailto:"]').forEach(function (link) {
      link.href = "contact.html";
      link.textContent = "Liên hệ";
    });

    const newsletter = document.querySelector(".footer-newsletter");
    if (newsletter) {
      const heading = newsletter.querySelector("h2");
      const description = newsletter.querySelector("p");
      if (heading) heading.textContent = "Thông tin cửa hàng";
      if (description) description.textContent = "Khám phá sản phẩm thời trang và chính sách mua hàng của eStore.";
      newsletter.querySelector("form")?.remove();
    }
    document.querySelectorAll(".footer-widget").forEach(function (widget) {
      if (/Download Our App|Tải ứng dụng/.test(widget.textContent)) widget.remove();
    });
    const paymentIcons = document.querySelector(".payment-icons");
    if (paymentIcons) {
      paymentIcons.innerHTML = '<i class="bi bi-cash" aria-label="Thanh toán khi nhận hàng"></i>';
      const label = paymentIcons.parentElement?.querySelector("span");
      if (label) label.textContent = "Thanh toán:";
    }
    document.querySelectorAll(".footer-contact .contact-item").forEach(function (item, index) {
      if (index === 0) item.innerHTML = '<i class="bi bi-geo-alt" aria-hidden="true"></i><span>Địa chỉ cửa hàng sẽ được cập nhật.</span>';
      if (index === 1) item.innerHTML = '<i class="bi bi-telephone" aria-hidden="true"></i><span>Vui lòng xem trang Liên hệ.</span>';
      if (index === 2) item.innerHTML = '<i class="bi bi-envelope" aria-hidden="true"></i><span>Kênh email hỗ trợ sẽ được cập nhật.</span>';
    });
  }

  function storedUser() {
    try {
      return JSON.parse(sessionStorage.getItem(userKey) || "null");
    } catch (error) {
      console.error("Không đọc được thông tin tài khoản trong phiên.", error);
      return null;
    }
  }

  function authHeaders() {
    const token = sessionStorage.getItem(tokenKey);
    return token ? { Authorization: "Bearer " + token } : {};
  }

  async function api(path, options) {
    const settings = options || {};
    const headers = Object.assign({ Accept: "application/json" }, authHeaders(), settings.headers || {});
    if (settings.body !== undefined) headers["Content-Type"] = "application/json";
    let response;
    try {
      response = await fetch(apiBase + path, Object.assign({}, settings, { headers: headers }));
    } catch (error) {
      throw new Error("Không kết nối được máy chủ cửa hàng. Hãy kiểm tra backend và cấu hình CORS rồi thử lại.");
    }

    let result;
    try {
      result = await response.json();
    } catch (error) {
      throw new Error("Máy chủ trả về dữ liệu không hợp lệ (HTTP " + response.status + ").");
    }
    if (!response.ok || result.success === false) {
      if (response.status >= 500) {
        throw new Error("Máy chủ gặp sự cố khi xử lý dữ liệu. Vui lòng thử lại sau.");
      }
      throw new Error(result.message || "Yêu cầu thất bại (HTTP " + response.status + ").");
    }
    return result.data;
  }

  function setMessage(element, message, isError) {
    if (!element) return;
    element.textContent = message || "";
    element.className = "store-message" + (message ? (isError ? " is-error" : " is-success") : "");
    element.setAttribute("role", isError ? "alert" : "status");
  }

  function formatPrice(value) {
    return money.format(Number(value) || 0);
  }

  function setBusy(button, busy, label) {
    if (!button) return;
    if (busy) {
      button.dataset.originalText = button.textContent;
      button.disabled = true;
      button.textContent = label || "Đang xử lý…";
    } else {
      button.disabled = false;
      button.textContent = button.dataset.originalText || button.textContent;
      delete button.dataset.originalText;
    }
  }

  function updateHeader(user) {
    const userNow = user || storedUser();
    document.querySelectorAll(".account-dropdown .dropdown-header h6").forEach(function (title) {
      title.innerHTML = userNow
        ? "Xin chào, <span class=\"sitename\">" + escapeHtml(userNow.fullName || userNow.username) + "</span>"
        : "Chào mừng đến <span class=\"sitename\">eStore</span>";
    });
    document.querySelectorAll(".account-dropdown .dropdown-body").forEach(function (body) {
      body.innerHTML = userNow
        ? '<a class="dropdown-item d-flex align-items-center" href="account.html"><i class="bi bi-person-circle me-2"></i><span>Tài khoản của tôi</span></a><a class="dropdown-item d-flex align-items-center" href="account.html"><i class="bi bi-bag-check me-2"></i><span>Đơn hàng của tôi</span></a>'
        : '<a class="dropdown-item d-flex align-items-center" href="login-register.html"><i class="bi bi-box-arrow-in-right me-2"></i><span>Đăng nhập để xem đơn hàng</span></a>';
    });
    document.querySelectorAll(".account-dropdown .dropdown-footer").forEach(function (footer) {
      footer.innerHTML = userNow
        ? '<a href="account.html" class="btn btn-primary w-100 mb-2">Tài khoản của tôi</a><button type="button" class="btn btn-outline-primary w-100" data-sign-out>Đăng xuất</button>'
        : '<a href="login-register.html" class="btn btn-primary w-100 mb-2">Đăng nhập</a><a href="login-register.html#register" class="btn btn-outline-primary w-100">Đăng ký</a>';
    });
    document.querySelectorAll(".header-action-btn .badge").forEach(function (badge) {
      if (badge.closest('a[href="cart.html"]')) {
        const saved = Number(sessionStorage.getItem("clothingShopCartCount") || 0);
        badge.textContent = String(saved);
        badge.setAttribute("aria-label", saved + " sản phẩm trong giỏ");
      }
    });
    document.querySelectorAll("[data-sign-out]").forEach(function (button) {
      button.addEventListener("click", function () {
        [tokenKey, userKey, "clothingShopCartCount"].forEach(function (key) { sessionStorage.removeItem(key); });
        window.location.href = "index.html";
      });
    });
  }

  function setupSearch() {
    document.querySelectorAll(".search-form").forEach(function (form) {
      const input = form.querySelector('input[type="search"], input[type="text"]');
      if (!input) return;
      input.placeholder = "Tìm kiếm sản phẩm";
      form.addEventListener("submit", function (event) {
        event.preventDefault();
        const keyword = input.value.trim();
        window.location.href = "search-results.html" + (keyword ? "?keyword=" + encodeURIComponent(keyword) : "");
      });
    });
  }

  function titleBlock(title, subtitle) {
    return '<div class="page-title light-background"><div class="container d-lg-flex justify-content-between align-items-center"><div><h1 class="mb-1">' +
      escapeHtml(title) + '</h1><p class="mb-0">' + escapeHtml(subtitle || "") +
      '</p></div><nav class="breadcrumbs" aria-label="Điều hướng"><ol><li><a href="index.html">Trang chủ</a></li><li class="current">' +
      escapeHtml(title) + "</li></ol></nav></div></div>";
  }

  function productCard(product) {
    const image = product.thumbnailUrl || "assets/img/product/product-1.webp";
    const available = Number(product.totalStock) > 0;
    return '<article class="col-sm-6 col-lg-4 col-xl-3"><div class="store-product-card h-100">' +
      '<a class="store-product-image" href="product-details.html?id=' + encodeURIComponent(product.id) + '">' +
      '<img src="' + escapeHtml(image) + '" alt="' + escapeHtml(product.productName) + '" loading="lazy">' +
      '</a><div class="store-product-info"><p class="text-muted mb-1">' + escapeHtml(product.brandName || "") + '</p>' +
      '<h2 class="h5"><a href="product-details.html?id=' + encodeURIComponent(product.id) + '">' + escapeHtml(product.productName) + '</a></h2>' +
      '<p class="store-product-price mb-1">' + formatPrice(product.minPrice) + '</p><p class="small mb-0 ' +
      (available ? "text-success" : "text-danger") + '">' + (available ? "Còn hàng" : "Tạm hết hàng") + "</p></div></div></article>";
  }

  function pageShell(title, subtitle, content) {
    const main = document.querySelector("main");
    if (!main) return null;
    main.innerHTML = titleBlock(title, subtitle) + '<section class="section"><div class="container">' + content + "</div></section>";
    return main;
  }

  async function loadProducts(keyword, gender) {
    const query = new URLSearchParams({ page: "1", size: "24" });
    if (keyword) query.set("keyword", keyword);
    if (gender !== null && gender !== undefined && gender !== "") query.set("gender", gender);
    const page = await api("/products?" + query.toString());
    const products = page.items || [];
    const content = '<form class="row g-2 mb-4" id="product-search"><div class="col"><label class="visually-hidden" for="product-keyword">Tìm sản phẩm</label>' +
      '<input class="form-control" id="product-keyword" name="keyword" value="' + escapeHtml(keyword || "") + '" placeholder="Nhập tên sản phẩm hoặc thương hiệu"></div>' +
      '<div class="col-auto"><button class="btn btn-primary" type="submit"><i class="bi bi-search me-1" aria-hidden="true"></i>Tìm kiếm</button></div>' +
      '<div class="col-auto"><label class="visually-hidden" for="product-gender">Lọc theo đối tượng</label><select class="form-select" id="product-gender" name="gender">' +
      '<option value="">Tất cả</option><option value="0">Nam</option><option value="1">Nữ</option><option value="2">Khác</option></select></div></form>' +
      '<p class="text-muted" id="product-count"></p><div class="row g-4" id="product-results">' +
      (products.length ? products.map(productCard).join("") : '<div class="col-12"><div class="alert alert-info">Chưa tìm thấy sản phẩm phù hợp.</div></div>') +
      "</div>";
    const shell = pageShell(keyword ? "Kết quả tìm kiếm" : "Sản phẩm", "Khám phá các sản phẩm đang được cửa hàng kinh doanh.", content);
    const count = shell.querySelector("#product-count");
    count.textContent = "Tìm thấy " + page.total + " sản phẩm";
    const genderSelect = shell.querySelector("#product-gender");
    genderSelect.value = gender == null ? "" : String(gender);
    shell.querySelector("#product-search").addEventListener("submit", function (event) {
      event.preventDefault();
      const formData = new FormData(event.currentTarget);
      const params = new URLSearchParams();
      const term = String(formData.get("keyword") || "").trim();
      const selectedGender = String(formData.get("gender") || "");
      if (term) params.set("keyword", term);
      if (selectedGender) params.set("gender", selectedGender);
      const queryString = params.toString();
      window.location.href = (keyword ? "search-results.html" : "category.html") + (queryString ? "?" + queryString : "");
    });
  }

  function currentUserId() {
    const user = storedUser();
    return user && user.customerId;
  }

  function cartPath() {
    return "/carts/customer/" + encodeURIComponent(currentUserId());
  }

  function showSignInNotice(title, subtitle) {
    pageShell(title, subtitle, '<div class="alert alert-info">Đăng nhập để xem thông tin và tiếp tục mua sắm.</div><a class="btn btn-primary" href="login-register.html">Đăng nhập</a> <a class="btn btn-outline-primary" href="login-register.html#register">Tạo tài khoản</a>');
  }

  async function renderProductDetail() {
    const id = new URLSearchParams(window.location.search).get("id");
    if (!id || !/^\d+$/.test(id)) {
      await loadProducts("", "");
      return;
    }
    const [product, variants] = await Promise.all([
      api("/products/" + encodeURIComponent(id)),
      api("/products/" + encodeURIComponent(id) + "/details")
    ]);
    const colors = Array.from(new Map(variants.map(function (item) { return [item.colorName, item]; })).values());
    const image = product.thumbnailUrl || (variants[0] && variants[0].imageUrl) || "assets/img/product/product-details-1.webp";
    const content = '<div class="row g-5"><div class="col-lg-6"><img class="img-fluid rounded-3 w-100" src="' + escapeHtml(image) +
      '" alt="' + escapeHtml(product.productName) + '"></div><div class="col-lg-6"><p class="text-muted mb-1">' + escapeHtml(product.brandName || "") +
      '</p><h2>' + escapeHtml(product.productName) + '</h2><p class="store-product-price fs-4">' + formatPrice(product.minPrice) +
      '</p><p>' + escapeHtml(product.materialName ? "Chất liệu: " + product.materialName : "Sản phẩm thời trang chất lượng.") +
      '</p><p class="text-success">Tổng tồn kho: ' + Number(product.totalStock || 0) + '</p><div class="mb-3"><label class="form-label" for="variant-color">Màu sắc</label>' +
      '<select class="form-select" id="variant-color">' + colors.map(function (color) { return '<option value="' + escapeHtml(color.colorName) + '">' + escapeHtml(color.colorName) + "</option>"; }).join("") +
      '</select></div><div class="mb-3"><label class="form-label" for="variant-size">Kích cỡ</label><select class="form-select" id="variant-size"></select>' +
      '<a class="d-inline-block mt-2" href="size-guide.html">Xem hướng dẫn chọn size</a></div><div class="mb-3"><label class="form-label" for="variant-quantity">Số lượng</label>' +
      '<input class="form-control" id="variant-quantity" type="number" value="1" min="1" max="100"></div><p class="store-message" id="product-message" role="status"></p>' +
      '<button class="btn btn-primary" id="add-to-cart" type="button">Thêm vào giỏ</button> <a class="btn btn-outline-secondary" href="category.html">Tiếp tục mua sắm</a></div></div>';
    const shell = pageShell("Chi tiết sản phẩm", "Thông tin sản phẩm và các lựa chọn màu sắc, kích cỡ.", content);
    const colorSelect = shell.querySelector("#variant-color");
    const sizeSelect = shell.querySelector("#variant-size");
    const quantity = shell.querySelector("#variant-quantity");
    const message = shell.querySelector("#product-message");
    function updateSizes() {
      const selectedVariants = variants.filter(function (variant) { return variant.colorName === colorSelect.value; });
      sizeSelect.innerHTML = selectedVariants.map(function (variant) {
        return '<option value="' + escapeHtml(variant.id) + '" data-stock="' + Number(variant.quantity || 0) + '">' +
          escapeHtml(variant.sizeName) + " — " + formatPrice(variant.price) + " (" + Number(variant.quantity || 0) + " còn)</option>";
      }).join("");
      shell.querySelector("#add-to-cart").disabled = !selectedVariants.some(function (variant) { return Number(variant.quantity) > 0; });
    }
    colorSelect.addEventListener("change", updateSizes);
    updateSizes();
    shell.querySelector("#add-to-cart").addEventListener("click", async function (event) {
      if (!currentUserId()) {
        window.location.href = "login-register.html?next=" + encodeURIComponent(window.location.pathname + window.location.search);
        return;
      }
      const button = event.currentTarget;
      const selectedVariant = variants.find(function (variant) { return String(variant.id) === sizeSelect.value; });
      const qty = Number(quantity.value);
      if (!selectedVariant || qty < 1 || qty > Number(selectedVariant.quantity)) {
        setMessage(message, "Vui lòng chọn biến thể còn hàng và số lượng phù hợp.", true);
        return;
      }
      setBusy(button, true);
      try {
        const cart = await api(cartPath() + "/items", {
          method: "POST",
          body: JSON.stringify({ productDetailId: selectedVariant.id, quantity: qty })
        });
        sessionStorage.setItem("clothingShopCartCount", String(cart.totalItems || 0));
        updateHeader();
        setMessage(message, "Đã thêm sản phẩm vào giỏ hàng.", false);
      } catch (error) {
        setMessage(message, error.message, true);
      } finally {
        setBusy(button, false);
      }
    });
  }

  async function renderCart() {
    if (!currentUserId()) return showSignInNotice("Giỏ hàng", "Đăng nhập để xem giỏ hàng của bạn.");
    const cart = await api(cartPath());
    sessionStorage.setItem("clothingShopCartCount", String(cart.totalItems || 0));
    const rows = (cart.items || []).map(function (item) {
      return '<div class="store-cart-row row align-items-center g-3 py-3 border-bottom" data-item-id="' + escapeHtml(item.id) + '">' +
        '<div class="col-4 col-md-2"><img class="img-fluid rounded" src="' + escapeHtml(item.imageUrl || "assets/img/product/product-1.webp") + '" alt="' + escapeHtml(item.productName) + '"></div>' +
        '<div class="col-8 col-md-4"><h2 class="h6 mb-1">' + escapeHtml(item.productName) + '</h2><p class="small text-muted mb-0">' + escapeHtml(item.colorName) + " / " + escapeHtml(item.sizeName) + '</p>' +
        '<p class="small mb-0">' + formatPrice(item.unitPrice) + " / sản phẩm</p></div><div class=\"col-6 col-md-3\"><label class=\"visually-hidden\" for=\"quantity-" + escapeHtml(item.id) + '\">Số lượng</label>' +
        '<input class="form-control store-cart-quantity" id="quantity-' + escapeHtml(item.id) + '" type="number" min="1" max="' + Math.max(1, Number(item.stock || 1)) + '" value="' + Number(item.quantity || 1) + '"></div>' +
        '<div class="col-4 col-md-2 fw-semibold">' + formatPrice(item.totalPrice) + '</div><div class="col-2 col-md-1"><button class="btn btn-outline-danger btn-sm" type="button" data-remove-item aria-label="Xóa sản phẩm"><i class="bi bi-trash" aria-hidden="true"></i></button></div></div>';
    }).join("");
    const content = (rows ? '<div id="cart-items">' + rows + '</div>' : '<div class="alert alert-info">Giỏ hàng đang trống.</div>') +
      '<div class="row justify-content-end mt-4"><div class="col-md-6 col-lg-4"><div class="border rounded-3 p-4"><h2 class="h5">Tóm tắt giỏ hàng</h2>' +
      '<div class="d-flex justify-content-between"><span>Tạm tính</span><strong id="cart-total">' + formatPrice(cart.totalAmount) + '</strong></div>' +
      '<p class="small text-muted mt-2">Phí giao hàng được tính khi xác nhận đơn hàng.</p><a class="btn btn-primary w-100" href="checkout.html"' +
      (cart.totalItems ? "" : ' aria-disabled="true" tabindex="-1"') + '>Tiến hành thanh toán</a></div></div></div>' +
      '<div class="mt-4"><a href="category.html">Tiếp tục mua sắm</a></div><p id="cart-message" class="store-message" role="status"></p>';
    const shell = pageShell("Giỏ hàng", "Kiểm tra sản phẩm và số lượng trước khi đặt hàng.", content);
    const message = shell.querySelector("#cart-message");
    shell.querySelectorAll(".store-cart-row").forEach(function (row) {
      const itemId = row.dataset.itemId;
      row.querySelector(".store-cart-quantity").addEventListener("change", async function (event) {
        const quantity = Math.min(100, Math.max(1, Number(event.currentTarget.value) || 1));
        event.currentTarget.value = String(quantity);
        try {
          await api(cartPath() + "/items/" + encodeURIComponent(itemId), {
            method: "PUT",
            body: JSON.stringify({ quantity: quantity })
          });
          await renderCart();
        } catch (error) {
          setMessage(message, error.message, true);
        }
      });
      row.querySelector("[data-remove-item]").addEventListener("click", async function () {
        try {
          await api(cartPath() + "/items/" + encodeURIComponent(itemId), { method: "DELETE" });
          await renderCart();
        } catch (error) {
          setMessage(message, error.message, true);
        }
      });
    });
    updateHeader();
  }

  function addressCard(address, index) {
    return '<label class="store-address-choice d-flex gap-3 p-3 border rounded-3 mb-2"><input type="radio" name="addressId" value="' +
      escapeHtml(address.id) + '"' + ((address.isDefault || index === 0) ? " checked" : "") + '><span><strong>' +
      escapeHtml(address.receiverName) + " · " + escapeHtml(address.receiverPhone) + '</strong><br>' +
      escapeHtml(address.detailAddress) + ", " + escapeHtml(address.ward) + ", " + escapeHtml(address.province) +
      (address.isDefault ? " · Mặc định" : "") + "</span></label>";
  }

  async function renderCheckout() {
    if (!currentUserId()) return showSignInNotice("Thanh toán", "Đăng nhập để chọn địa chỉ và đặt hàng.");
    const [cart, addresses] = await Promise.all([api(cartPath()), api("/addresses")]);
    if (!cart.totalItems) {
      return pageShell("Thanh toán", "Xác nhận đơn hàng của bạn.", '<div class="alert alert-info">Giỏ hàng đang trống. <a href="category.html">Xem sản phẩm</a></div>');
    }
    const savedAddresses = addresses.map(addressCard).join("");
    const content = '<div class="row g-4"><div class="col-lg-7"><h2 class="h4">Địa chỉ nhận hàng</h2>' +
      '<div id="saved-addresses">' + (savedAddresses || '<p class="text-muted">Bạn chưa lưu địa chỉ nhận hàng. Hãy nhập địa chỉ bên dưới.</p>') + '</div>' +
      '<details class="mt-3" ' + (addresses.length ? "" : "open") + '><summary class="mb-3">Thêm địa chỉ giao hàng</summary><div class="row g-3">' +
      '<div class="col-md-6"><label class="form-label" for="receiver-name">Tên người nhận</label><input class="form-control" id="receiver-name" maxlength="150"></div>' +
      '<div class="col-md-6"><label class="form-label" for="receiver-phone">Số điện thoại</label><input class="form-control" id="receiver-phone" maxlength="20"></div>' +
      '<div class="col-md-6"><label class="form-label" for="province">Tỉnh / Thành phố</label><input class="form-control" id="province" maxlength="100"></div>' +
      '<div class="col-md-6"><label class="form-label" for="ward">Phường / Xã</label><input class="form-control" id="ward" maxlength="100"></div>' +
      '<div class="col-12"><label class="form-label" for="detail-address">Địa chỉ cụ thể</label><input class="form-control" id="detail-address" maxlength="300" placeholder="Số nhà, tên đường"></div>' +
      '<div class="col-12"><div class="form-check"><input class="form-check-input" id="default-address" type="checkbox"><label class="form-check-label" for="default-address">Lưu làm địa chỉ mặc định</label></div></div>' +
      '<div class="col-12"><button class="btn btn-outline-primary" id="save-address" type="button">Lưu địa chỉ</button></div></div></details>' +
      '<h2 class="h4 mt-4">Ghi chú đơn hàng</h2><label class="visually-hidden" for="order-note">Ghi chú</label><textarea class="form-control" id="order-note" rows="3" maxlength="500" placeholder="Ví dụ: giao giờ hành chính"></textarea></div>' +
      '<div class="col-lg-5"><div class="border rounded-3 p-4"><h2 class="h4">Đơn hàng</h2><ul class="list-group list-group-flush mb-3">' +
      cart.items.map(function (item) { return '<li class="list-group-item d-flex justify-content-between gap-3 px-0"><span>' + escapeHtml(item.productName) + " · " +
        escapeHtml(item.colorName) + " / " + escapeHtml(item.sizeName) + " × " + Number(item.quantity) + "</span><strong>" + formatPrice(item.totalPrice) + "</strong></li>"; }).join("") +
      '</ul><div class="d-flex justify-content-between"><span>Tạm tính</span><strong>' + formatPrice(cart.totalAmount) + '</strong></div><p class="small text-muted mt-2">Phí giao hàng sẽ được cộng theo cấu hình cửa hàng.</p>' +
      '<div class="mb-3"><label class="form-label" for="payment-method">Thanh toán</label><select class="form-select" id="payment-method"><option value="COD">Thanh toán khi nhận hàng (COD)</option></select></div>' +
      '<p class="store-message" id="checkout-message" role="status"></p><button class="btn btn-primary w-100" id="place-order" type="button">Đặt hàng</button></div></div></div>';
    const shell = pageShell("Thanh toán", "Chọn địa chỉ nhận hàng và phương thức thanh toán.", content);
    const message = shell.querySelector("#checkout-message");
    let currentAddresses = addresses.slice();
    shell.querySelector("#save-address").addEventListener("click", async function (event) {
      const button = event.currentTarget;
      const address = {
        receiverName: shell.querySelector("#receiver-name").value.trim(),
        receiverPhone: shell.querySelector("#receiver-phone").value.trim(),
        province: shell.querySelector("#province").value.trim(),
        ward: shell.querySelector("#ward").value.trim(),
        detailAddress: shell.querySelector("#detail-address").value.trim(),
        isDefault: shell.querySelector("#default-address").checked
      };
      if (Object.values(address).some(function (value) { return typeof value === "string" && !value; })) {
        setMessage(message, "Vui lòng nhập đầy đủ thông tin địa chỉ.", true);
        return;
      }
      setBusy(button, true);
      try {
        const created = await api("/addresses", { method: "POST", body: JSON.stringify(address) });
        currentAddresses.push(created);
        shell.querySelector("#saved-addresses")?.insertAdjacentHTML("beforeend", addressCard(created, currentAddresses.length - 1));
        shell.querySelector("details").open = false;
        shell.querySelectorAll('input[name="addressId"]').forEach(function (input) { input.checked = String(input.value) === String(created.id); });
        setMessage(message, "Đã lưu địa chỉ nhận hàng.", false);
      } catch (error) {
        setMessage(message, error.message, true);
      } finally {
        setBusy(button, false);
      }
    });
    shell.querySelector("#place-order").addEventListener("click", async function (event) {
      const addressId = shell.querySelector('input[name="addressId"]:checked')?.value;
      if (!addressId) {
        setMessage(message, "Vui lòng chọn hoặc thêm địa chỉ nhận hàng.", true);
        return;
      }
      const button = event.currentTarget;
      setBusy(button, true);
      try {
        const bill = await api(cartPath() + "/checkout", {
          method: "POST",
          body: JSON.stringify({
            addressId: Number(addressId),
            paymentMethod: "COD",
            note: shell.querySelector("#order-note").value.trim()
          })
        });
        sessionStorage.setItem(orderKey, String(bill.id));
        sessionStorage.setItem("clothingShopCartCount", "0");
        window.location.href = "order-confirmation.html?orderId=" + encodeURIComponent(bill.id);
      } catch (error) {
        setMessage(message, error.message, true);
        setBusy(button, false);
      }
    });
  }

  async function renderAuth() {
    const content = '<div class="row justify-content-center"><div class="col-lg-8"><ul class="nav nav-tabs mb-4" role="tablist">' +
      '<li class="nav-item"><button class="nav-link active" id="login-tab" type="button">Đăng nhập</button></li>' +
      '<li class="nav-item"><button class="nav-link" id="register-tab" type="button">Tạo tài khoản</button></li></ul>' +
      '<p class="store-message" id="auth-message" role="status"></p><form id="login-form"><h2 class="h4 mb-3">Đăng nhập</h2>' +
      '<div class="mb-3"><label class="form-label" for="login-email">Email</label><input class="form-control" id="login-email" type="email" autocomplete="email" required></div>' +
      '<div class="mb-3"><label class="form-label" for="login-password">Mật khẩu</label><input class="form-control" id="login-password" type="password" autocomplete="current-password" required></div>' +
      '<button class="btn btn-primary" type="submit">Đăng nhập</button></form><form id="register-form" hidden><h2 class="h4 mb-3">Tạo tài khoản</h2>' +
      '<div class="mb-3"><label class="form-label" for="register-username">Tên đăng nhập</label><input class="form-control" id="register-username" minlength="3" maxlength="100" autocomplete="username" required></div>' +
      '<div class="mb-3"><label class="form-label" for="register-name">Họ và tên</label><input class="form-control" id="register-name" maxlength="150" autocomplete="name" required></div>' +
      '<div class="mb-3"><label class="form-label" for="register-email">Email</label><input class="form-control" id="register-email" type="email" maxlength="255" autocomplete="email" required></div>' +
      '<div class="mb-3"><label class="form-label" for="register-phone">Số điện thoại (không bắt buộc)</label><input class="form-control" id="register-phone" autocomplete="tel"></div>' +
      '<div class="mb-3"><label class="form-label" for="register-password">Mật khẩu (ít nhất 8 ký tự)</label><input class="form-control" id="register-password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required></div>' +
      '<button class="btn btn-primary" type="submit">Tạo tài khoản</button></form></div></div>';
    const shell = pageShell("Tài khoản", "Đăng nhập hoặc đăng ký để quản lý đơn hàng.", content);
    const loginForm = shell.querySelector("#login-form");
    const registerForm = shell.querySelector("#register-form");
    const message = shell.querySelector("#auth-message");
    const loginTab = shell.querySelector("#login-tab");
    const registerTab = shell.querySelector("#register-tab");
    function showRegister(show) {
      loginForm.hidden = show;
      registerForm.hidden = !show;
      loginTab.classList.toggle("active", !show);
      registerTab.classList.toggle("active", show);
      if (show) history.replaceState(null, "", "#register");
    }
    loginTab.addEventListener("click", function () { showRegister(false); });
    registerTab.addEventListener("click", function () { showRegister(true); });
    if (window.location.hash === "#register") showRegister(true);

    async function authenticate(form, endpoint, payload) {
      const button = form.querySelector('button[type="submit"]');
      setBusy(button, true);
      setMessage(message, "", false);
      try {
        const auth = await api(endpoint, { method: "POST", body: JSON.stringify(payload) });
        sessionStorage.setItem(tokenKey, auth.accessToken);
        sessionStorage.setItem(userKey, JSON.stringify(auth.user));
        updateHeader(auth.user);
        const next = new URLSearchParams(window.location.search).get("next");
        window.location.href = next && next.startsWith("/") ? next : "account.html";
      } catch (error) {
        setMessage(message, error.message, true);
        setBusy(button, false);
      }
    }
    loginForm.addEventListener("submit", function (event) {
      event.preventDefault();
      authenticate(loginForm, "/auth/login", {
        email: shell.querySelector("#login-email").value.trim(),
        password: shell.querySelector("#login-password").value
      });
    });
    registerForm.addEventListener("submit", function (event) {
      event.preventDefault();
      authenticate(registerForm, "/auth/register", {
        username: shell.querySelector("#register-username").value.trim(),
        fullName: shell.querySelector("#register-name").value.trim(),
        email: shell.querySelector("#register-email").value.trim(),
        phone: shell.querySelector("#register-phone").value.trim(),
        password: shell.querySelector("#register-password").value
      });
    });
  }

  function orderStatus(status) {
    return ({
      0: "Chờ xác nhận",
      1: "Đã xác nhận",
      2: "Đang giao",
      3: "Hoàn thành",
      4: "Đã hủy",
      5: "Yêu cầu trả hàng"
    })[status] || "Đang cập nhật";
  }

  async function renderAccount() {
    if (!currentUserId()) return showSignInNotice("Tài khoản của tôi", "Đăng nhập để xem hồ sơ và đơn hàng.");
    const [user, bills] = await Promise.all([api("/auth/me"), api("/bills")]);
    sessionStorage.setItem(userKey, JSON.stringify(user));
    const orders = bills.length ? '<div class="table-responsive"><table class="table align-middle"><thead><tr><th>Mã đơn</th><th>Ngày đặt</th><th>Trạng thái</th><th>Tổng tiền</th><th></th></tr></thead><tbody>' +
      bills.map(function (bill) {
        return '<tr><td>' + escapeHtml(bill.billCode) + '</td><td>' + escapeHtml(new Date(bill.createdAt).toLocaleDateString("vi-VN")) +
          '</td><td>' + orderStatus(bill.status) + '</td><td>' + formatPrice(bill.totalAmount) + '</td><td><button class="btn btn-sm btn-outline-primary" type="button" data-order-id="' +
          escapeHtml(bill.id) + '">Chi tiết</button>' + (bill.status === 0 ? ' <button class="btn btn-sm btn-outline-danger" type="button" data-cancel-id="' + escapeHtml(bill.id) + '">Hủy</button>' : "") + "</td></tr>";
      }).join("") + "</tbody></table></div>" : '<div class="alert alert-info">Bạn chưa có đơn hàng nào.</div>';
    const content = '<div class="row g-4"><div class="col-lg-4"><div class="border rounded-3 p-4"><h2 class="h5">Thông tin tài khoản</h2><p class="mb-1"><strong>' +
      escapeHtml(user.fullName) + '</strong></p><p class="mb-1">' + escapeHtml(user.email) + '</p><p class="mb-0">' + escapeHtml(user.phone || "Chưa cập nhật số điện thoại") + '</p></div></div>' +
      '<div class="col-lg-8"><h2 class="h4">Đơn hàng của tôi</h2><p class="store-message" id="account-message" role="status"></p>' + orders + '</div></div>';
    const shell = pageShell("Tài khoản của tôi", "Thông tin cá nhân và lịch sử mua hàng.", content);
    const message = shell.querySelector("#account-message");
    shell.querySelectorAll("[data-order-id]").forEach(function (button) {
      button.addEventListener("click", function () {
        window.location.href = "order-confirmation.html?orderId=" + encodeURIComponent(button.dataset.orderId);
      });
    });
    shell.querySelectorAll("[data-cancel-id]").forEach(function (button) {
      button.addEventListener("click", async function () {
        if (!window.confirm("Bạn có chắc muốn hủy đơn hàng này?")) return;
        setBusy(button, true);
        try {
          await api("/bills/" + encodeURIComponent(button.dataset.cancelId) + "/cancel", { method: "PATCH" });
          await renderAccount();
        } catch (error) {
          setMessage(message, error.message, true);
          setBusy(button, false);
        }
      });
    });
  }

  async function renderOrderConfirmation() {
    if (!currentUserId()) return showSignInNotice("Đơn hàng", "Đăng nhập để theo dõi đơn hàng.");
    const requestedId = new URLSearchParams(window.location.search).get("orderId") || sessionStorage.getItem(orderKey);
    if (!requestedId || !/^\d+$/.test(requestedId)) {
      return pageShell("Đơn hàng của tôi", "Theo dõi trạng thái các đơn đã đặt.", '<div class="alert alert-info">Chưa có đơn hàng cần hiển thị. <a href="account.html">Xem đơn hàng của tôi</a></div>');
    }
    const [bill, lines, history] = await Promise.all([
      api("/bills/" + encodeURIComponent(requestedId)),
      api("/bills/" + encodeURIComponent(requestedId) + "/lines"),
      api("/bills/" + encodeURIComponent(requestedId) + "/history")
    ]);
    const content = '<div class="alert alert-success"><h2 class="h4">Đã tiếp nhận đơn hàng</h2><p class="mb-0">Mã đơn: <strong>' + escapeHtml(bill.billCode) +
      '</strong> · Trạng thái: ' + orderStatus(bill.status) + '</p></div><div class="row g-4"><div class="col-lg-7"><h3 class="h5">Sản phẩm</h3>' +
      '<ul class="list-group mb-4">' + lines.map(function (line) {
        return '<li class="list-group-item d-flex justify-content-between gap-3"><span>' + escapeHtml(line.productName) + " · " + escapeHtml(line.colorName) + " / " +
          escapeHtml(line.sizeName) + " × " + Number(line.quantity) + "</span><strong>" + formatPrice(line.totalPrice) + "</strong></li>";
      }).join("") + '</ul><h3 class="h5">Cập nhật đơn hàng</h3><ul class="list-group">' + history.map(function (entry) {
        return '<li class="list-group-item">' + escapeHtml(entry.note || orderStatus(entry.status)) + ' <span class="text-muted small">' +
          escapeHtml(new Date(entry.createdAt).toLocaleString("vi-VN")) + "</span></li>";
      }).join("") + '</ul></div><div class="col-lg-5"><div class="border rounded-3 p-4"><h3 class="h5">Thông tin giao hàng</h3><p>' +
      escapeHtml(bill.receiverName) + " · " + escapeHtml(bill.receiverPhone) + "<br>" + escapeHtml(bill.shippingAddress) +
      '</p><p>Thanh toán: ' + escapeHtml(bill.paymentMethod === "COD" ? "Khi nhận hàng (COD)" : bill.paymentMethod) +
      '</p><div class="d-flex justify-content-between"><span>Tạm tính</span><span>' + formatPrice(bill.subTotal) +
      '</span></div><div class="d-flex justify-content-between"><span>Giao hàng</span><span>' + formatPrice(bill.shippingFee) +
      '</span></div><hr><div class="d-flex justify-content-between fw-bold"><span>Tổng cộng</span><span>' + formatPrice(bill.totalAmount) +
      '</span></div></div></div></div><a class="btn btn-outline-primary mt-4" href="account.html">Về đơn hàng của tôi</a>';
    pageShell("Chi tiết đơn hàng", "Thông tin trạng thái và giao nhận.", content);
  }

  function renderError(error) {
    const main = document.querySelector("main");
    if (!main) return;
    main.innerHTML = titleBlock("Không tải được dữ liệu", "Đã xảy ra lỗi khi kết nối hoặc đọc dữ liệu từ cửa hàng.") +
      '<section class="section"><div class="container"><div class="alert alert-danger" role="alert">' + escapeHtml(error.message) +
      '</div><button class="btn btn-outline-primary" type="button" onclick="window.location.reload()">Thử lại</button></div></section>';
  }

  function renderStaticPage(filename) {
    const pages = {
      "about.html": {
        title: "Giới thiệu",
        subtitle: "Cửa hàng thời trang trực tuyến eStore.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><h2 class="h4">Thời trang cho phong cách hằng ngày</h2><p>eStore giúp bạn khám phá các sản phẩm quần áo, xem thông tin màu sắc, kích cỡ và tình trạng hàng trước khi đặt mua.</p><p>Thông tin sản phẩm và đơn hàng được tải trực tiếp từ hệ thống cửa hàng. Nếu cần tư vấn về kích cỡ, hãy xem <a href="size-guide.html">hướng dẫn chọn size</a> hoặc liên hệ với cửa hàng.</p><a class="btn btn-primary" href="category.html">Xem sản phẩm</a></div></div>'
      },
      "contact.html": {
        title: "Liên hệ",
        subtitle: "Thông tin hỗ trợ mua hàng.",
        content: '<div class="row g-4"><div class="col-lg-7"><h2 class="h4">Cần hỗ trợ?</h2><p>Để được hỗ trợ nhanh, vui lòng chuẩn bị mã đơn hàng (nếu có) và đăng nhập để xem trạng thái đơn trong mục tài khoản.</p><a class="btn btn-primary me-2" href="account.html">Đơn hàng của tôi</a><a class="btn btn-outline-primary" href="faq.html">Xem câu hỏi thường gặp</a></div><div class="col-lg-5"><div class="border rounded-3 p-4"><h2 class="h5">Kênh liên hệ</h2><p class="mb-0">Cửa hàng chưa cấu hình số điện thoại hoặc email hỗ trợ trên website. Vui lòng cập nhật thông tin liên hệ chính thức trước khi đưa trang vào hoạt động.</p></div></div></div>'
      },
      "faq.html": {
        title: "Câu hỏi thường gặp",
        subtitle: "Thông tin nhanh về sản phẩm, thanh toán và đơn hàng.",
        content: '<div class="accordion" id="store-faq"><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#faq-size">Làm sao để chọn đúng kích cỡ?</button></h2><div id="faq-size" class="accordion-collapse collapse show" data-bs-parent="#store-faq"><div class="accordion-body">Đo vòng ngực, eo và hông rồi đối chiếu <a href="size-guide.html">bảng size tham khảo</a>. Nếu sản phẩm có hướng dẫn riêng, hãy ưu tiên thông tin trên trang sản phẩm.</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-payment">Website hiện hỗ trợ thanh toán nào?</button></h2><div id="faq-payment" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Hiện hệ thống chỉ hỗ trợ thanh toán khi nhận hàng (COD).</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-shipping">Phí giao hàng là bao nhiêu?</button></h2><div id="faq-shipping" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Phí mặc định hiện là 30.000 đồng và có thể được cửa hàng cấu hình lại. Tổng tiền và phí giao hàng được hiển thị trong thông tin đơn.</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-cancel">Tôi có thể hủy đơn hàng không?</button></h2><div id="faq-cancel" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Bạn có thể gửi yêu cầu hủy khi đơn còn ở trạng thái chờ xác nhận, trong mục <a href="account.html">đơn hàng của tôi</a>.</div></div></div><div class="accordion-item"><h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#faq-status">Theo dõi đơn hàng ở đâu?</button></h2><div id="faq-status" class="accordion-collapse collapse" data-bs-parent="#store-faq"><div class="accordion-body">Đăng nhập rồi mở mục tài khoản để xem trạng thái, chi tiết và lịch sử cập nhật đơn hàng.</div></div></div></div>'
      },
      "payment-methods.html": {
        title: "Phương thức thanh toán",
        subtitle: "Phương thức thanh toán hiện được hệ thống hỗ trợ.",
        content: '<div class="border rounded-3 p-4"><h2 class="h4">Thanh toán khi nhận hàng (COD)</h2><p>Khách hàng thanh toán cho đơn vị giao hàng khi nhận sản phẩm. Vui lòng kiểm tra thông tin người nhận và số điện thoại trước khi đặt hàng.</p><p class="mb-0">Thanh toán trực tuyến bằng thẻ, ví điện tử hoặc chuyển khoản chưa được tích hợp.</p></div>'
      },
      "privacy.html": {
        title: "Chính sách quyền riêng tư",
        subtitle: "Thông tin về dữ liệu được sử dụng để vận hành việc mua hàng.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><p>Website sử dụng thông tin tài khoản, địa chỉ nhận hàng và nội dung đơn hàng để cung cấp các chức năng đăng nhập, giao hàng và tra cứu đơn.</p><h2 class="h5 mt-4">Dữ liệu tài khoản</h2><p>Mật khẩu được lưu ở dạng mã hóa một chiều trong hệ thống. Không gửi mật khẩu hoặc mã truy cập cho người khác.</p><h2 class="h5 mt-4">Quản lý thông tin</h2><p>Thông tin đơn hàng cần được giữ để cửa hàng xử lý và hỗ trợ sau khi đặt mua. Các yêu cầu cập nhật hoặc xóa dữ liệu cần được cửa hàng xử lý theo chính sách áp dụng.</p><div class="alert alert-warning">Cửa hàng cần rà soát và bổ sung thông tin pháp lý, thời hạn lưu trữ và kênh tiếp nhận yêu cầu trước khi vận hành chính thức.</div></div></div>'
      },
      "return-policy.html": {
        title: "Chính sách đổi trả",
        subtitle: "Các thao tác hiện được hỗ trợ trên hệ thống.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><h2 class="h4">Hủy đơn hàng</h2><p>Khách hàng có thể gửi yêu cầu hủy đơn khi đơn còn ở trạng thái chờ xác nhận. Mở mục <a href="account.html">đơn hàng của tôi</a>, chọn đơn phù hợp và nhấn hủy.</p><h2 class="h5 mt-4">Đổi hoặc trả sản phẩm</h2><p>Yêu cầu đổi hoặc trả sau khi nhận hàng chưa được xử lý trực tiếp trên website. Vui lòng liên hệ cửa hàng để xác nhận điều kiện và quy trình áp dụng.</p><div class="alert alert-warning">Cửa hàng cần công bố thời hạn, điều kiện đổi trả và thông tin liên hệ chính thức trước khi nhận đơn hàng.</div></div></div>'
      },
      "shiping-info.html": {
        title: "Thông tin giao hàng",
        subtitle: "Thông tin giao nhận đang được áp dụng trong hệ thống.",
        content: '<div class="row g-4"><div class="col-md-6"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Phí giao hàng</h2><p class="mb-0">Phí mặc định là 30.000 đồng mỗi đơn và có thể thay đổi theo cấu hình của cửa hàng. Phí được tính vào tổng đơn hàng khi thanh toán.</p></div></div><div class="col-md-6"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Theo dõi đơn hàng</h2><p class="mb-0">Đăng nhập và mở mục <a href="account.html">đơn hàng của tôi</a> để xem trạng thái và lịch sử cập nhật.</p></div></div><div class="col-12"><div class="alert alert-warning mb-0">Thời gian giao hàng và đơn vị vận chuyển chưa được tích hợp; cửa hàng cần cập nhật chính sách giao nhận thực tế.</div></div></div>'
      },
      "support.html": {
        title: "Trung tâm hỗ trợ",
        subtitle: "Hướng dẫn sử dụng các chức năng mua hàng.",
        content: '<div class="row g-4"><div class="col-md-4"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Chọn sản phẩm</h2><p>Xem màu sắc, kích cỡ, giá và tình trạng hàng trên trang chi tiết sản phẩm.</p><a href="size-guide.html">Hướng dẫn chọn size</a></div></div><div class="col-md-4"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Thanh toán</h2><p>Đặt hàng với địa chỉ nhận hàng hợp lệ. Hiện chỉ hỗ trợ thanh toán khi nhận hàng.</p><a href="payment-methods.html">Chi tiết thanh toán</a></div></div><div class="col-md-4"><div class="h-100 border rounded-3 p-4"><h2 class="h5">Đơn hàng</h2><p>Đăng nhập để xem trạng thái, lịch sử và yêu cầu hủy đơn đang chờ xác nhận.</p><a href="account.html">Mở tài khoản</a></div></div></div><p class="mt-4">Cần thêm trợ giúp? Xem <a href="faq.html">câu hỏi thường gặp</a> hoặc <a href="contact.html">thông tin liên hệ</a>.</p>'
      },
      "tos.html": {
        title: "Điều khoản sử dụng",
        subtitle: "Các nguyên tắc cơ bản khi sử dụng website mua hàng.",
        content: '<div class="row justify-content-center"><div class="col-lg-9"><h2 class="h5">Thông tin sản phẩm</h2><p>Giá, kích cỡ và tình trạng tồn kho được hiển thị theo dữ liệu cửa hàng. Đơn hàng chỉ được tiếp nhận khi hệ thống xác nhận thành công.</p><h2 class="h5 mt-4">Tài khoản</h2><p>Bạn cần cung cấp thông tin nhận hàng chính xác và bảo vệ thông tin đăng nhập của mình.</p><h2 class="h5 mt-4">Thanh toán và hủy đơn</h2><p>Phương thức thanh toán hiện hỗ trợ là COD. Đơn hàng chỉ có thể được hủy khi đang chờ xác nhận.</p><h2 class="h5 mt-4">Chính sách cửa hàng</h2><p>Các điều khoản pháp lý, quyền và nghĩa vụ cụ thể cần được cửa hàng rà soát và bổ sung trước khi website được đưa vào vận hành chính thức.</p></div></div>'
      },
      "blog.html": {
        title: "Cẩm nang mặc đẹp",
        subtitle: "Gợi ý chọn và chăm sóc trang phục.",
        content: '<div class="row g-4"><div class="col-md-6"><article class="border rounded-3 p-4 h-100"><p class="text-muted">Hướng dẫn</p><h2 class="h4"><a href="blog-details.html">Cách chọn kích cỡ quần áo phù hợp</a></h2><p>Đo số đo cơ thể, đối chiếu bảng size và lưu ý phom dáng để chọn trang phục thoải mái.</p><a href="size-guide.html">Xem bảng size</a></article></div><div class="col-md-6"><article class="border rounded-3 p-4 h-100"><p class="text-muted">Chăm sóc trang phục</p><h2 class="h4">Đọc nhãn hướng dẫn giặt trước khi sử dụng</h2><p>Phân loại màu sắc và làm theo hướng dẫn trên nhãn để giữ trang phục bền đẹp.</p></article></div></div>'
      },
      "blog-details.html": {
        title: "Cách chọn kích cỡ quần áo phù hợp",
        subtitle: "Hướng dẫn chọn size cơ bản.",
        content: '<article class="row justify-content-center"><div class="col-lg-9"><p>Để chọn kích cỡ phù hợp, hãy dùng thước dây mềm và đo trực tiếp trên cơ thể. Giữ thước vừa sát, không siết chặt.</p><h2 class="h5 mt-4">Đối chiếu số đo</h2><p>Đo vòng ngực, vòng eo và vòng hông. Với áo, ưu tiên vòng ngực; với quần hoặc chân váy, chú ý vòng eo và hông.</p><h2 class="h5 mt-4">Chọn phom mặc</h2><p>Nếu số đo nằm giữa hai mức, cân nhắc chọn size lớn hơn để mặc thoải mái. Bảng size của từng sản phẩm (nếu có) luôn được ưu tiên.</p><a class="btn btn-primary mt-2" href="size-guide.html">Mở hướng dẫn chọn size</a></div></article>'
      },
      "starter-page.html": {
        title: "Thông tin cửa hàng",
        subtitle: "Trang giới thiệu các chức năng mua sắm.",
        content: '<div class="row g-3"><div class="col-md-4"><a class="d-block border rounded-3 p-4 h-100" href="category.html"><h2 class="h5">Sản phẩm</h2><p class="mb-0">Tìm kiếm và xem các sản phẩm đang kinh doanh.</p></a></div><div class="col-md-4"><a class="d-block border rounded-3 p-4 h-100" href="size-guide.html"><h2 class="h5">Chọn kích cỡ</h2><p class="mb-0">Tham khảo hướng dẫn đo và bảng size.</p></a></div><div class="col-md-4"><a class="d-block border rounded-3 p-4 h-100" href="account.html"><h2 class="h5">Đơn hàng</h2><p class="mb-0">Đăng nhập để theo dõi các đơn đã đặt.</p></a></div></div>'
      },
      "404.html": {
        title: "Không tìm thấy trang",
        subtitle: "Đường dẫn bạn truy cập không tồn tại hoặc đã được thay đổi.",
        content: '<div class="text-center"><i class="bi bi-search display-4 text-muted" aria-hidden="true"></i><p class="mt-3">Hãy quay về trang chủ hoặc xem danh sách sản phẩm.</p><a class="btn btn-primary me-2" href="index.html">Về trang chủ</a><a class="btn btn-outline-primary" href="category.html">Xem sản phẩm</a></div>'
      }
    };
    const page = pages[filename];
    if (page) pageShell(page.title, page.subtitle, page.content);
  }

  function selectedView() {
    const file = window.location.pathname.split("/").pop().toLowerCase();
    if (file === "index.html" || file === "") return ["products", "", ""];
    if (file === "category.html") return ["products", "", new URLSearchParams(window.location.search).get("gender")];
    if (file === "search-results.html") return ["products", new URLSearchParams(window.location.search).get("keyword") || "", new URLSearchParams(window.location.search).get("gender")];
    if (file === "product-details.html") return ["product"];
    if (file === "cart.html") return ["cart"];
    if (file === "checkout.html") return ["checkout"];
    if (file === "login-register.html") return ["auth"];
    if (file === "account.html") return ["account"];
    if (file === "order-confirmation.html") return ["order"];
    if (["about.html", "contact.html", "faq.html", "payment-methods.html", "privacy.html", "return-policy.html",
      "shiping-info.html", "support.html", "tos.html", "blog.html", "blog-details.html", "starter-page.html", "404.html"].includes(file)) {
      return ["static", file];
    }
    return null;
  }

  async function mountStorefront() {
    localizeStaticText();
    updateHeader();
    setupSearch();
    const view = selectedView();
    if (!view) return;
    try {
      if (view[0] === "products") await loadProducts(view[1], view[2]);
      else if (view[0] === "product") await renderProductDetail();
      else if (view[0] === "cart") await renderCart();
      else if (view[0] === "checkout") await renderCheckout();
      else if (view[0] === "auth") await renderAuth();
      else if (view[0] === "account") await renderAccount();
      else if (view[0] === "order") await renderOrderConfirmation();
      else if (view[0] === "static") renderStaticPage(view[1]);
      updateHeader();
    } catch (error) {
      console.error("Không thể tải dữ liệu cửa hàng.", error);
      renderError(error);
    }
  }

  mountStorefront();
})();
