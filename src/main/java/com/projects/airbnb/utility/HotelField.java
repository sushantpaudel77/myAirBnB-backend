package com.projects.airbnb.utility;

import lombok.Getter;

@Getter
public enum HotelField {
    HOTEL("Hotel"),
    ROOM("Room");

    private final String key;

    HotelField(String key) {
        this.key = key;
    }
}
