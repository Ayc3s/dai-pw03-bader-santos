package ch.heigvd.clients;

public record Client(
    Integer id,
    String firstName,
    String lastName,
    String email,
    String phone
) {}
