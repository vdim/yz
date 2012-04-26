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
public class NotDefinedDPException extends YZException {

    public NotDefinedDPException(Throwable cause) {
        super(cause);
    }

    public NotDefinedDPException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotDefinedDPException(String message) {
        super(message);
    }

    public NotDefinedDPException() {
        super();
    }

}
