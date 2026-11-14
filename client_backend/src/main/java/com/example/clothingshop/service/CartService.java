package com.example.clothingshop.service;

import com.example.clothingshop.common.BusinessException;
import com.example.clothingshop.common.NotFoundException;
import com.example.clothingshop.dto.request.BillRequest;
import com.example.clothingshop.dto.request.CartCheckoutRequest;
import com.example.clothingshop.dto.request.CartItemRequest;
import com.example.clothingshop.dto.request.CartItemUpdateRequest;
import com.example.clothingshop.entity.Cart;
import com.example.clothingshop.entity.CartDetail;
import com.example.clothingshop.repositories.CartRepository;
import com.example.clothingshop.view.BillView;
import com.example.clothingshop.view.CartView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository repo;
    private final BillService billService;

    public CartService(CartRepository repo, BillService billService) {
        this.repo = repo;
        this.billService = billService;
    }

    /** Xem giỏ. Khách chưa có giỏ -> trả giỏ rỗng (không tạo mới khi chỉ xem). */
    public CartView get(long customerId) {
        ensureCustomer(customerId);
        return repo.findActiveCart(customerId)
                .map(cart -> CartView.of(cart, repo.findItemViews(cart.id())))
                .orElseGet(() -> CartView.empty(customerId));
    }

    /** Thêm vào giỏ; nếu biến thể đã có trong giỏ thì cộng dồn số lượng. */
    @Transactional
    public CartView addItem(long customerId, CartItemRequest req) {
        ensureCustomer(customerId);
        int stock = repo.findStock(req.productDetailId())
                .orElseThrow(() -> new BusinessException("Sản phẩm chi tiết không tồn tại hoặc đã ngừng bán: "
                        + req.productDetailId()));

        long cartId = repo.findActiveCart(customerId).map(Cart::id)
                .orElseGet(() -> repo.insertCart(customerId));

        var existing = repo.findDetailByProduct(cartId, req.productDetailId());
        int newQty = req.quantity() + existing.map(CartDetail::quantity).orElse(0);
        checkStock(newQty, stock);

        if (existing.isPresent()) {
            repo.updateDetailQuantity(existing.get().id(), newQty);
        } else {
            repo.insertDetail(cartId, req.productDetailId(), newQty);
        }
        return get(customerId);
    }

    /** Đặt lại số lượng của 1 dòng. */
    @Transactional
    public CartView updateItem(long customerId, long itemId, CartItemUpdateRequest req) {
        CartDetail item = getItem(customerId, itemId);
        int stock = repo.findStock(item.productDetailId())
                .orElseThrow(() -> new BusinessException("Sản phẩm này đã ngừng bán, hãy xóa khỏi giỏ"));
        checkStock(req.quantity(), stock);
        repo.updateDetailQuantity(itemId, req.quantity());
        return get(customerId);
    }

    @Transactional
    public CartView removeItem(long customerId, long itemId) {
        getItem(customerId, itemId);
        repo.softDeleteDetail(itemId);
        return get(customerId);
    }

    @Transactional
    public CartView clear(long customerId) {
        ensureCustomer(customerId);
        repo.findActiveCart(customerId).ifPresent(cart -> repo.softDeleteAllDetails(cart.id()));
        return get(customerId);
    }

    /**
     * Đặt hàng từ giỏ: lấy các dòng trong giỏ -> BillService.create (trừ kho, snapshot địa chỉ...)
     * -> đánh dấu giỏ đã checkout. Cùng 1 transaction: lỗi ở bước nào thì rollback hết.
     */
    @Transactional
    public BillView checkout(long customerId, CartCheckoutRequest req) {
        return createBillFromCart(customerId, req);
    }

    @Transactional
    public BillView checkoutSePay(long customerId, CartCheckoutRequest req) {
        if (!"SEPAY".equals(req.paymentMethod())) {
            throw new BusinessException("Phương thức thanh toán không hợp lệ cho luồng SePay");
        }
        return createBillFromCart(customerId, req);
    }

    private BillView createBillFromCart(long customerId, CartCheckoutRequest req) {
        ensureCustomer(customerId);
        Cart cart = repo.findActiveCart(customerId)
                .orElseThrow(() -> new BusinessException("Giỏ hàng đang trống"));
        List<CartDetail> details = repo.findDetails(cart.id());
        if (details.isEmpty()) throw new BusinessException("Giỏ hàng đang trống");

        List<BillRequest.Item> items = details.stream()
                .map(d -> new BillRequest.Item(d.productDetailId(), d.quantity()))
                .toList();
        BillRequest billReq = new BillRequest(req.addressId(), req.paymentMethod(), req.note(), items);

        BillView bill = billService.create(customerId, billReq);
        repo.markCheckedOut(cart.id());
        return bill;
    }

    // ---------- tiện ích ----------

    private void ensureCustomer(long customerId) {
        if (!repo.customerExists(customerId)) {
            throw new NotFoundException("Không tìm thấy khách hàng id=" + customerId);
        }
    }

    private CartDetail getItem(long customerId, long itemId) {
        return repo.findDetailOfCustomer(itemId, customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dòng giỏ hàng id=" + itemId));
    }

    private void checkStock(int wanted, int stock) {
        if (wanted > stock) {
            throw new BusinessException("Không đủ hàng: yêu cầu " + wanted + ", trong kho còn " + stock);
        }
    }
}
