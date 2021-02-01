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
    private String currency = "GBP";
    private float minorUnits = 0;
}