package com.lorenadev.app.comercios_solidatios_tus.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lorenadev.app.comercios_solidatios_tus.entity.CommerceEntity;

import java.util.List;

@Dao
public interface CommerceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CommerceEntity> commerces);

    @Query("SELECT * FROM commerces")
    List<CommerceEntity> getAll();

    @Query("SELECT * FROM commerces WHERE lat BETWEEN :latMin AND :latMax AND lng BETWEEN :lngMin AND :lngMax")
    List<CommerceEntity> findInBoundingBox(double latMin, double latMax, double lngMin, double lngMax);

}
