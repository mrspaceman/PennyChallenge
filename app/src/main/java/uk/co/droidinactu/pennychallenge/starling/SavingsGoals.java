package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** */
@Data
public class SavingsGoals {
  private List<SavingsGoal> savingsGoals = new ArrayList<>();

  public void addSavingsGoal(SavingsGoal act) {
    savingsGoals.add(act);
  }
}
