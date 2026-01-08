package ch.heigvd.baths;

public record Measurement(
    Integer bathId,
    Double temperature,
    String measuredAt
) {}
