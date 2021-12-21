package uk.co.droidinactu.pennychallenge.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

@Entity
public class SavedOnDate {
  @PrimaryKey
  @ColumnInfo(name = "date_saved")
  public LocalDate dateSaved;

  @ColumnInfo(name = "amount_saved")
  public int amount;
}
