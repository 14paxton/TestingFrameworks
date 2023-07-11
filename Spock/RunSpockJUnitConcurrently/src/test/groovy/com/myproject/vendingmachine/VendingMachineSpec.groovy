package com.myproject.vendingmachine
import spock.lang.Specification

class VendingMachineSpec extends Specification{


    def "test boolean" () {
        setup:
        def vend1 = new VendingMachine(true)
        def vend2 = VendingMachine.builder().on(false).build()

        expect:
        vend1.on
        !vend2.on

        when:
        vend1.powerButton()
        vend2.powerButton()

        then:
        !vend1.on
        vend2.on
    }

}
