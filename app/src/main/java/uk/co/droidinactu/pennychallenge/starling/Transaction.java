package uk.co.droidinactu.pennychallenge.starling;

import lombok.Builder;
import lombok.Data;

/**
 * { "feedItemUid": "uid", "categoryUid": "uid", "amount": { "currency": "GBP", "minorUnits": 1135 *
 * }, "sourceAmount": { "currency": "GBP", "minorUnits": 1135 }, "direction": "OUT", "updatedAt": *
 * "2021-12-14T06:03:08.208Z", "transactionTime": "2021-12-14T06:03:07.002Z", "settlementTime": *
 * "2021-12-14T06:03:08.119Z", "source": "FASTER_PAYMENTS_OUT", "status": "SETTLED",
 * "transactingApplicationUserUid": "uid", "counterPartyType": "PAYEE", "counterPartyUid": "uid",
 * "counterPartyName": "other party", "counterPartySubEntityUid": "uid",
 * "counterPartySubEntityName": "GB Account", "counterPartySubEntityIdentifier": "404765",
 * "counterPartySubEntitySubIdentifier": "60264024", "reference": "for other party", "country":
 * "GB", "spendingCategory": "PAYMENTS", "hasAttachment": false, "hasReceipt": false }
 */
@Data
@Builder
public class Transaction {
  String feedItemUid;
  String categoryUid;
  CurrencyAndAmount amount;
  CurrencyAndAmount sourceAmount;
  String direction;
  String updatedAt;
  String transactionTime;
  String settlementTime;
  String source;
  String status;
  String transactingApplicationUserUid;
  String counterPartyType;
  String counterPartyUid;
  String counterPartyName;
  String counterPartySubEntityUid;
  String counterPartySubEntityName;
  String counterPartySubEntityIdentifier;
  String counterPartySubEntitySubIdentifier;
  String reference;
  String country;
  String spendingCategory;
  String hasAttachment;
  String hasReceipt;
}
