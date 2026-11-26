package com.groupt1.hms.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReportStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ReportStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData.equalsIgnoreCase("Open")) {
            return ReportStatus.PENDING;
        } else if (dbData.equalsIgnoreCase("In Progress")) {
            return ReportStatus.PROCESSING;
        } else if (dbData.equalsIgnoreCase("Resolved")) {
            return ReportStatus.DONE;
        }
        try {
            return ReportStatus.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ReportStatus.PENDING;
        }
    }
}
