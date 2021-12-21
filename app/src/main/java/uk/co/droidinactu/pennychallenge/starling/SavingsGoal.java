package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

/**
 * { "savingsGoalUid": "77887788-7788-7788-7788-778877887788", "name": "Trip to Paris", "target": {
 * "currency": "GBP", "minorUnits": 123456 }, "totalSaved": { "currency": "GBP", "minorUnits":
 * 123456 }, "savedPercentage": 100 }
 */
@Data
public class SavingsGoal {

  private String savingsGoalUid;
  private String name;
  private int savedPercentage;
  private CurrencyAndAmount target;
  private CurrencyAndAmount totalSaved;

  public SavingsGoal(String pennyChallenge) {
    this.name = pennyChallenge;
  }
}
