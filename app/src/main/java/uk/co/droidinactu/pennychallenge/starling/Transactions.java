package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Transactions {

  private List<Transaction> feedItems = new ArrayList<>();

  public void addTransaction(Transaction act) {
    feedItems.add(act);
  }
}
