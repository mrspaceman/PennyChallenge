package uk.co.droidinactu.pennychallenge.starling;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 *
 */
@Data
public class SavingsGoals {
    private List<SavingsGoal> savingsGoals = new ArrayList<>();

    public void addSavingsGoal(SavingsGoal act) {
        savingsGoals.add(act);
    }
}
