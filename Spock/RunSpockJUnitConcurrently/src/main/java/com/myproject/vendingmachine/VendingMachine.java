package com.myproject.vendingmachine;

import lombok.*;

@Builder(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public @Data class VendingMachine {
    boolean on;

    public void powerButton() {
        on = !on;
    }

    public boolean isOn() {
        return on;
    }

}
