package com.fixfinder.modelos.componentes;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EstadoOperarioConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        return (attribute != null && attribute) ? "DISPONIBLE" : "BAJA";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return "DISPONIBLE".equalsIgnoreCase(dbData);
    }
}
