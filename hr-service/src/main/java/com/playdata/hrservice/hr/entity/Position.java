package com.playdata.hrservice.hr.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

public enum Position {
    INTERN,
    JUNIOR,
    SENIOR,
    MANAGER,
    DIRECTOR,
    CEO


//    private final String displayName;
//
//    Position(String displayName) {
//        this.displayName = displayName;
//    }
//
//    public static Position fromDisplayName(String name) {
//        for (Position p : values()) {
//            if (p.displayName.equals(name)) {
//                return p;
//            }
//
//        }
//        throw new IllegalArgumentException("존재하지 않는 직급: " + name);
//    }
}