package ch.heigvd.baths;

public record MeasurementRequest(
    Double temperature,
    String measuredAt
) {}
