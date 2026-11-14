package com.example.clothingshop.view;

import java.time.LocalDateTime;

public record BillHistoryView(Long id, Integer oldStatus, Integer newStatus,
                              String note, LocalDateTime createdAt) {}
