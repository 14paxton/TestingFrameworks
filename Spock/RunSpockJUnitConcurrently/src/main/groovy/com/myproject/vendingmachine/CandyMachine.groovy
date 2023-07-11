package com.myproject.vendingmachine
import groovy.json.JsonBuilder

class CandyMachine extends VendingMachine {
    Long id
    List<Candy> listOfCandy
    
    public CandyMachine(listOfCandy, id){
        this.listOfCandy = listOfCandy
        this.id = id
    }
    
    @Override
    String toString(){
    return "{$id : ${listOfCandy.join(',')}}"
    }
}
