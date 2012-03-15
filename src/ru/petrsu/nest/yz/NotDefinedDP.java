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
public class NotDefinedDP extends YZException {

    public NotDefinedDP(Throwable cause) {
        super(cause);
    }

    public NotDefinedDP(String message, Throwable cause) {
        super(message, cause);
    }

    public NotDefinedDP(String message) {
        super(message);
    }

    public NotDefinedDP() {
        super();
    }

}
