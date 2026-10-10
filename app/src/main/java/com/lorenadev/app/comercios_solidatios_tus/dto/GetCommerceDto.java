package com.lorenadev.app.comercios_solidatios_tus.dto;

public record GetCommerceDto(
        String midesId,
        double lat,
        double lng,
        String name,
        String address,
        String phone,
        String category,
        String subCategory
) {}
