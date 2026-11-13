package com.example.clothingshop.repositories;

import com.example.clothingshop.common.SqlHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillRepositoryTest {

    private RecordingSqlHelper sql;

    private BillRepository repository;

    @BeforeEach
    void setUp() {
        sql = new RecordingSqlHelper();
        repository = new BillRepository(sql);
    }

    @Test
    void reserveStockDecreasesAvailableAndIncreasesReservedAtomically() {
        sql.updateResults.add(1);

        assertEquals(1, repository.reserveStock(345, 1));

        assertEquals(Map.of("id", 345L, "qty", 1), sql.parameters.get(0));
        assertTrue(sql.statements.get(0).contains("QUANTITY = QUANTITY - :qty"));
        assertTrue(sql.statements.get(0).contains("RESERVED_QUANTITY = RESERVED_QUANTITY + :qty"));
        assertTrue(sql.statements.get(0).contains("QUANTITY >= :qty"));
    }

    @Test
    void releaseReservationUpdatesQuantityAndReservedOnlyOnce() {
        sql.updateResults.addAll(List.of(1, 1));
        sql.expectedProducts = 1L;

        repository.releaseReservedStockForBill(81);

        assertTrue(sql.statements.get(0).contains("STOCK_RESERVED = 0"));
        assertTrue(sql.statements.get(1).contains("RESERVED_QUANTITY = RESERVED_QUANTITY -"));
        assertTrue(sql.statements.get(1).contains("QUANTITY = QUANTITY +"));
    }

    @Test
    void releaseReservationDoesNothingWhenBillHasNoActiveReservation() {
        sql.updateResults.add(0);

        repository.releaseReservedStockForBill(81);

        assertEquals(1, sql.statements.size());
        assertEquals(0, sql.scalarCalls);
    }

    @Test
    void releaseReservationFailsWhenNotAllBillProductsCanBeReleased() {
        sql.updateResults.addAll(List.of(1, 0));
        sql.expectedProducts = 1L;

        Assertions.assertThrows(IllegalStateException.class,
                () -> repository.releaseReservedStockForBill(81));
    }

    private static final class RecordingSqlHelper extends SqlHelper {
        private final List<String> statements = new ArrayList<>();
        private final List<Map<String, ?>> parameters = new ArrayList<>();
        private final Deque<Integer> updateResults = new ArrayDeque<>();
        private Long expectedProducts;
        private int scalarCalls;

        private RecordingSqlHelper() {
            super(null);
        }

        @Override
        public int update(String statement, Map<String, ?> parameters) {
            this.statements.add(statement);
            this.parameters.add(parameters);
            return updateResults.removeFirst();
        }

        @Override
        public <T> Optional<T> scalar(String statement, Map<String, ?> parameters, Class<T> type) {
            scalarCalls++;
            return Optional.of(type.cast(expectedProducts));
        }
    }
}
