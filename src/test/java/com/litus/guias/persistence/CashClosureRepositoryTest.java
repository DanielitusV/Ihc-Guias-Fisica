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

import static org.junit.jupiter.api.Assertions.*;

class CashClosureRepositoryTest {

    @TempDir
    Path tempDir;

    private Database database;
    private long guideId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(
                "jdbc:sqlite:" + tempDir.resolve("test.db")
        );

        database.initialize();

        GuideRepository guideRepository =
                new GuideRepository(database);

        guideId = guideRepository.save(
                new Guide(
                        0,
                        "Física I",
                        new BigDecimal("35.00"),
                        10
                )
        );
    }

    @Test
    void savesClosureWithItems() throws Exception {
        CashClosureRepository repository =
                new CashClosureRepository(database);

        CashClosure closure = createClosure();

        List<CashClosureItem> items = List.of(
                new CashClosureItem(
                        0,
                        0,
                        guideId,
                        10,
                        10
                )
        );

        long id = repository.save(closure, items);

        CashClosure found = repository.findById(id);

        List<CashClosureItem> foundItems =
                repository.findItemsByClosureId(id);

        assertNotNull(found);
        assertEquals(1, foundItems.size());
        assertEquals(
                0,
                foundItems.getFirst().getDifference()
        );
    }

    @Test
    void persistsClosureCancellation() throws Exception {
        CashClosureRepository repository =
                new CashClosureRepository(database);

        long id = repository.save(
                createClosure(),
                List.of(
                        new CashClosureItem(
                                0,
                                0,
                                guideId,
                                10,
                                10
                        )
                )
        );

        CashClosure closure =
                repository.findById(id);

        closure.cancel(
                "Cierre realizado por error"
        );

        repository.update(closure);

        CashClosure found =
                repository.findById(id);

        assertEquals(
                CashClosureStatus.CANCELLED,
                found.getStatus()
        );

        assertEquals(
                "Cierre realizado por error",
                found.getCancellationReason()
        );
    }

    @Test
    void findsValidClosureByDate() throws Exception {
        CashClosureRepository repository =
                new CashClosureRepository(database);

        long id = repository.save(
                createClosure(),
                List.of(
                        new CashClosureItem(
                                0,
                                0,
                                guideId,
                                10,
                                10
                        )
                )
        );

        CashClosure found =
                repository.findValidByDate(
                        LocalDate.of(2026, 8, 15)
                );

        assertNotNull(found);
        assertEquals(id, found.getId());
        assertEquals(
                CashClosureStatus.VALID,
                found.getStatus()
        );
    }

    private CashClosure createClosure() {
        return new CashClosure(
                0,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("70.00"),
                new BigDecimal("70.00"),
                "Todo correcto",
                LocalDateTime.of(
                        2026,
                        8,
                        15,
                        21,
                        30
                )
        );
    }
}