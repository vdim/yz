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
public class NotFoundPropertyException extends YZException {

    public NotFoundPropertyException(Throwable cause) {
        super(cause);
    }

    public NotFoundPropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundPropertyException(String message) {
        super(message);
    }

    public NotFoundPropertyException() {
        super();
    }

}
