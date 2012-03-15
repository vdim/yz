/*
 *  Copyright (C) 2011-2012 Petrozavodsk State University
 *
 *  This file is part of YZ.
 */
package ru.petrsu.nest.yz;

/**
 *
 * @author Vyacheslav Dimitrov
 */
public class NotFoundFunctionException extends YZException {

    public NotFoundFunctionException(Throwable cause) {
        super(cause);
    }

    public NotFoundFunctionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundFunctionException(String message) {
        super(message);
    }

    public NotFoundFunctionException() {
        super();
    }

}
