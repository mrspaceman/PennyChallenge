package uk.co.droidinactu.pennychallenge.starling;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * { "title": "Mr", "firstName": "Dave", "lastName": "Bowman", "dateOfBirth": "1968-04-02", "email":
 * "dave.bowman@example.com", "phone": "+447700900123" }
 */
@Data
public class Individual {

  private String accountUid;
  private String accountType;
  private String defaultCategory;
  private String currency;
  private LocalDateTime createdAt;
  private String name;
}
