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
public class NotFoundPathException extends YZException {

    public NotFoundPathException(Throwable cause) {
        super(cause);
    }

    public NotFoundPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundPathException(String message) {
        super(message);
    }

    public NotFoundPathException() {
        super();
    }

}
