package com.playdata.noticeservice.notice.entity;

public enum Position {
    INTERN,
    JUNIOR,
    SENIOR,
    MANAGER,
    DIRECTOR,
    CEO;

    public int Position(Position position) {
        switch (position) {
            case INTERN -> {return INTERN.ordinal();}
            case JUNIOR -> {return JUNIOR.ordinal();}
            case SENIOR -> {return SENIOR.ordinal();}
            case MANAGER -> {return MANAGER.ordinal();}
            case DIRECTOR -> {return DIRECTOR.ordinal();}
            case CEO -> {return CEO.ordinal();}
        }
        return 0;
    }

}