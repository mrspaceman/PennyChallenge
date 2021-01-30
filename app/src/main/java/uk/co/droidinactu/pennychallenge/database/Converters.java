package uk.co.droidinactu.pennychallenge.database;

import androidx.room.TypeConverter;

import java.time.LocalDate;

public class Converters {

    @TypeConverter
    public static LocalDate fromTimestamp(Long value) {
        return value == null ? null : LocalDate.ofEpochDay(value);
    }

    @TypeConverter
    public static Long dateToString(LocalDate date) {
        return date == null ? null : date.toEpochDay();
    }

}
