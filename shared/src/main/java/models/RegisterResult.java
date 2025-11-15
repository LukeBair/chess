package models;

public record RegisterResult(String username, String authToken, String message) {} // message null on success