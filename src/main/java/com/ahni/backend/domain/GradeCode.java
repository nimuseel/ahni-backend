package com.ahni.backend.domain;

import java.math.BigDecimal;

public enum GradeCode {
    A_PLUS("4.50"),
    A_ZERO("4.00"),
    B_PLUS("3.50"),
    B_ZERO("3.00"),
    C_PLUS("2.50"),
    C_ZERO("2.00"),
    D_PLUS("1.50"),
    D_ZERO("1.00"),
    F("0.00"),
    P(null),
    NP(null);

    private final BigDecimal gradePoint;

    GradeCode(String gradePoint) {
        this.gradePoint = gradePoint == null ? null : new BigDecimal(gradePoint);
    }

    public BigDecimal gradePoint() {
        return gradePoint;
    }
}
