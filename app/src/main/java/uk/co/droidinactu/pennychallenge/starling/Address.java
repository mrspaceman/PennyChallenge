package uk.co.droidinactu.pennychallenge.starling;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * {
 * "current": {
 * "line1": "1A Admiralty Arch",
 * "line2": "The Mall",
 * "line3": "City of Westminster",
 * "postTown": "London",
 * "postCode": "SW1A 2WH",
 * "countryCode": "GB"
 * },
 * "previous": [
 * {
 * "line1": "1A Admiralty Arch",
 * "line2": "The Mall",
 * "line3": "City of Westminster",
 * "postTown": "London",
 * "postCode": "SW1A 2WH",
 * "countryCode": "GB"
 * }
 * ]
 * }
 */
@Data
public class Address {

    private String accountUid;
    private String accountType;
    private String defaultCategory;
    private String currency;
    private LocalDateTime createdAt;
    private String name;

}
