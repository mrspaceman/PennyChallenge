package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

/**
 * {
 * "clearedBalance": {
 * "currency": "GBP",
 * "minorUnits": 123456
 * },
 * "effectiveBalance": {
 * "currency": "GBP",
 * "minorUnits": 123456
 * },
 * "pendingTransactions": {
 * "currency": "GBP",
 * "minorUnits": 123456
 * },
 * "acceptedOverdraft": {
 * "currency": "GBP",
 * "minorUnits": 123456
 * },
 * "amount": {
 * "currency": "GBP",
 * "minorUnits": 123456
 * }
 * }
 */
@Data
public class AccountBalance {

    private CurrencyAndAmount clearedBalance;
    private CurrencyAndAmount effectiveBalance;
    private CurrencyAndAmount pendingTransactions;
    private CurrencyAndAmount acceptedOverdraft;
    private CurrencyAndAmount amount;

}
