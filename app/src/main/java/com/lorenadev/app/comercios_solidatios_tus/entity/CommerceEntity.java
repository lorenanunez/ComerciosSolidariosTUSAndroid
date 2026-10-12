package com.lorenadev.app.comercios_solidatios_tus.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.lorenadev.app.comercios_solidatios_tus.dto.GetCommerceDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity(tableName = "commerces")
@NoArgsConstructor
@AllArgsConstructor
public class CommerceEntity {

    @PrimaryKey
    @NonNull
    private String midesId;

    private Double lat;
    private Double lng;
    private String name;
    private String address;
    private String phone;
    private String category;
    private String subCategory;

    public static CommerceEntity fromDto(GetCommerceDto e) {
        return CommerceEntity.builder()
                .midesId(e.midesId())
                .lat(e.lat())
                .lng(e.lng())
                .name(e.name())
                .address(e.address())
                .phone(e.phone())
                .category(e.category())
                .subCategory(e.subCategory())
                .build();
    }

}
