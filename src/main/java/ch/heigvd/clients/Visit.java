package ch.heigvd.clients;

public record Visit(
    Integer id,
    Integer clientId,
    Integer bathId,
    String visitedAt
) {}
