package ch.heigvd.baths;

public record Bath(
    Integer id,
    String name,
    String location,
    String type,
    Boolean maintenanceDone,
    Double minTemperature,
    Double maxTemperature,
    Boolean isActive
) {}
