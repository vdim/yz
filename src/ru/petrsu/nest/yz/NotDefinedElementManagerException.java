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
public class NotDefinedElementManagerException extends YZException {

    public NotDefinedElementManagerException(Throwable cause) {
        super(cause);
    }

    public NotDefinedElementManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotDefinedElementManagerException(String message) {
        super(message);
    }

    public NotDefinedElementManagerException() {
        super();
    }

}
