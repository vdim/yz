/*
 *  Copyright (C) 2012 Petrozavodsk State University
 *
 *  This file is part of YZ.
 */
package ru.petrsu.nest.yz;

/**
 *
 * @author Vyacheslav Dimitrov
 */
abstract public class YZException extends Exception {
    public YZException(Throwable cause) {
        super(cause);
    }

    public YZException(String message, Throwable cause) {
        super(message, cause);
    }

    public YZException(String message) {
        super(message);
    }

    public YZException() {
        super();
    }
}
