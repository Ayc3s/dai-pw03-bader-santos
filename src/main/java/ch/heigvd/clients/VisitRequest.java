package ch.heigvd.clients;

public record VisitRequest(
    Integer bathId,
    String visitedAt
) {}
