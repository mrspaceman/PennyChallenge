package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * { "accountUid": "bbccbbcc-bbcc-bbcc-bbcc-bbccbbccbbcc", "accountType": "PRIMARY",
 * "defaultCategory": "ccddccdd-ccdd-ccdd-ccdd-ccddccddccdd", "currency": "GBP", "createdAt":
 * "2021-01-23T11:51:29.315Z", "name": "Personal" }
 */
@Data
public class Account {

  private String accountUid;
  private String accountType;
  private String defaultCategory;
  private String currency;
  private LocalDateTime createdAt;
  private String name;
}
