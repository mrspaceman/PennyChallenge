package uk.co.droidinactu.pennychallenge.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.List;

@Dao
public interface SavedOnDateDao {

    @Query("SELECT * FROM SavedOnDate")
    List<SavedOnDate> getAll();

    @Query("SELECT * FROM SavedOnDate where date_saved=:date")
    List<SavedOnDate> get(LocalDate date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(SavedOnDate... users);

    @Delete
    void delete(SavedOnDate user);

    @Update
    void updateSavedOnDate(SavedOnDate... users);
}
