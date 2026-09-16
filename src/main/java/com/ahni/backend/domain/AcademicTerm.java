package com.ahni.backend.domain;

public enum AcademicTerm {
    FIRST(1),
    SUMMER(2),
    SECOND(3),
    WINTER(4);

    private final int sequence;

    AcademicTerm(int sequence) {
        this.sequence = sequence;
    }

    public int sequence() {
        return sequence;
    }
}
