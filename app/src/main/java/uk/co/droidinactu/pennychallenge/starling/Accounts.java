package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Accounts {
  private List<Account> accounts = new ArrayList<>();

  public void addAccount(Account act) {
    accounts.add(act);
  }
}
