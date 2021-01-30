package uk.co.droidinactu.pennychallenge.starling;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Accounts {
    private List<Account> accounts = new ArrayList<>();

    public void addAccount(Account act) {
        accounts.add(act);
    }
}
