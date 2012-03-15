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
public class SyntaxException extends YZException {

    public SyntaxException(Throwable cause) {
        super(cause);
    }

    public SyntaxException(String message, Throwable cause) {
        super("SyntaxException: " + message, cause);
    }

    public SyntaxException(String message) {
        super("SyntaxException: " + message);
    }

    public SyntaxException() {
        super();
    }

}
