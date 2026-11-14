package com.example.clothingshop.common;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

    /** Đổi kiểu phần tử (ví dụ entity -> view) nhưng giữ nguyên thông tin phân trang. */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(items.stream().<R>map(mapper).toList(), page, size, total, totalPages);
    }
}
