package com.myproject.vendingmachine

import groovy.json.JsonBuilder
import lombok.AccessLevel
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.NoArgsConstructor

@Builder(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
 class Candy {
    Long id
    String name
    Integer calories

    @Override
    String toString() {
        new JsonBuilder(this)
    }
}
