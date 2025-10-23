package server.Req_Res;

public record LoginResult(String username, String authToken, String message) {} // message null on success