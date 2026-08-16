package com.litus.guias.persistence;

import com.litus.guias.account.CashClosureItem;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.CashClosureStatus;
import com.litus.guias.inventory.Guide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashClosureTransactionServiceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private CashClosureRepository repository;
    private CashClosureTransactionService service;
    private long guideId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        repository = new CashClosureRepository(database);
        service = new CashClosureTransactionService(database);
        guideId = new GuideRepository(database).save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 10)
        );
    }

    @Test
    void persistsClosureAndItemsAtomically() throws Exception {
        long id = service.registerClosure(
                closureAt(20),
                List.of(new CashClosureItem(0, 0, guideId, 10, 9))
        );

        assertNotNull(repository.findById(id));
        assertEquals(1, repository.findItemsByClosureId(id).size());
        assertEquals(1, repository.findAllByDate(LocalDate.of(2026, 8, 15)).size());
    }

    @Test
    void cancelledClosureAllowsReplacementAndKeepsHistory() throws Exception {
        long firstId = service.registerClosure(
                closureAt(18),
                List.of(new CashClosureItem(0, 0, guideId, 10, 10))
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.registerClosure(
                        closureAt(19),
                        List.of(new CashClosureItem(0, 0, guideId, 10, 10))
                )
        );

        service.cancelClosure(firstId, "Cierre prematuro");
        long replacementId = service.registerClosure(
                closureAt(20),
                List.of(new CashClosureItem(0, 0, guideId, 10, 10))
        );

        assertEquals(
                CashClosureStatus.CANCELLED,
                repository.findById(firstId).getStatus()
        );
        assertEquals(
                replacementId,
                repository.findValidByDate(LocalDate.of(2026, 8, 15)).getId()
        );
        assertEquals(2, repository.findAllByDate(LocalDate.of(2026, 8, 15)).size());
    }

    @Test
    void invalidItemRollsBackClosure() {
        assertThrows(
                Exception.class,
                () -> service.registerClosure(
                        closureAt(20),
                        List.of(new CashClosureItem(0, 0, 9999, 10, 10))
                )
        );

        assertNull(assertDoesNotThrowValidClosure());
    }

    private CashClosure assertDoesNotThrowValidClosure() {
        try {
            return repository.findValidByDate(LocalDate.of(2026, 8, 15));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private CashClosure closureAt(int hour) {
        return new CashClosure(
                0,
                new BigDecimal("100.00"),
                new BigDecimal("95.00"),
                new BigDecimal("50.00"),
                new BigDecimal("52.00"),
                "Cierre",
                LocalDateTime.of(2026, 8, 15, hour, 0)
        );
    }
}
