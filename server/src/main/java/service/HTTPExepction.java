package service;

import model.ErrorModel;

public class HTTPExepction extends RuntimeException {
    public final ErrorModel model;
    public final int statusCode;
    public HTTPExepction(ErrorModel errorModel, int statusCode) {
        super("");
        model = errorModel;
        this.statusCode = statusCode;
    }
}
