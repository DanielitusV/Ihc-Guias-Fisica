package com.litus.guias;

import java.util.List;

public class CashClosureService {

    public void registerClosure(
            CashClosure newClosure,
            List<CashClosure> existingClosures
    ) {
        for (CashClosure closure : existingClosures) {
            boolean sameDay =
                    closure.getCreatedAt().toLocalDate()
                            .equals(newClosure.getCreatedAt().toLocalDate()
                            );

            boolean isValid =
                    closure.getStatus() == CashClosureStatus.VALID;

            if (sameDay && isValid) {
                throw new IllegalStateException(
                        "A valid cash closure already exists for this day"
                );
            }
        }
    }
}
