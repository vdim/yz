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
public class NotFoundElementException extends YZException {

    public NotFoundElementException(Throwable cause) {
        super(cause);
    }

    public NotFoundElementException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundElementException(String message) {
        super(message);
    }

    public NotFoundElementException() {
        super();
    }

}
