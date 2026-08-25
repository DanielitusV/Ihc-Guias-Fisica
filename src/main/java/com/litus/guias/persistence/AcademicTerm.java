package com.litus.guias.persistence;

import java.time.LocalDateTime;

public record AcademicTerm(
        long id,
        String code,
        AcademicTermStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
}
