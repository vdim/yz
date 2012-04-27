/*
 *  Copyright (C) 2012 Petrozavodsk State University
 *
 *  This file is part of YZ.
 */
package ru.petrsu.nest.yz;

import java.lang.annotation.*;

/**
 * Defines whether field is default property. 
 * Not that YZ supports syntax sugar for default property into
 * where, projection and sorting clauses:
 *	floor#(.=1)
 *	floor[&.]
 *	{a:&.}floor
 *
 * @author Vyacheslav Dimitrov
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultProperty {
}

