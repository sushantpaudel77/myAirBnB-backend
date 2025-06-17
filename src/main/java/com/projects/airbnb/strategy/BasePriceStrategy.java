package com.projects.airbnb.strategy;

import com.projects.airbnb.entity.Inventory;

import java.math.BigDecimal;

public class BasePriceStrategy implements PricingStrategy{

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        return inventory.getRoom().getBasePrice();
    }
}
