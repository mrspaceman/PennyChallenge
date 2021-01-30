package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

/**
 * {
 * "currency": "GBP",
 * "minorUnits": 123456
 * }
 */
@Data
public class CurrencyAndAmount {
    private String currency;
    private float minorUnits;
}