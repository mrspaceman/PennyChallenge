package uk.co.droidinactu.pennychallenge.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
    entities = {SavedOnDate.class},
    version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
  public abstract SavedOnDateDao savedOnDateDao();
}
