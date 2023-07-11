package com.myproject.vendingmachine;
import org.junit.*;
import static org.junit.Assert.*;

public class VendingMachineTest {

    VendingMachine vm;
    @Before
    public void setUp() {
        vm = new VendingMachine(false);
    }

    @Test
    public void vmShouldTurnOnOffWhenSwitchedOnOff() {
        assertFalse(vm.isOn());

        vm.powerButton();

        assertFalse(vm.isOn());

        vm.powerButton();

        assertFalse(vm.isOn());
    }

}