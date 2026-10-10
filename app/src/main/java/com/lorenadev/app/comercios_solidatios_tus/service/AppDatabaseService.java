package com.lorenadev.app.comercios_solidatios_tus.service;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.lorenadev.app.comercios_solidatios_tus.dao.CommerceDao;
import com.lorenadev.app.comercios_solidatios_tus.entity.CommerceEntity;

@Database(entities = {CommerceEntity.class}, version = 1)
public abstract class AppDatabaseService extends RoomDatabase {

    public abstract CommerceDao commerceDao();

    public static AppDatabaseService getInstance(Context context) {
        return Holder.build(context);
    }

    private static final class Holder {
        private static AppDatabaseService instance;

        private static AppDatabaseService build(Context context) {
            if (instance == null) {
                synchronized (AppDatabaseService.class) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabaseService.class, "commerces.db").build();
                }
            }
            return instance;
        }
    }

}
