/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.util.EventListener;

/**
 * A "PropertyAccess" event gets fired whenever some property is accessed 
 * using get or set methods. You can register a PropertyAccessListener with a 
 * source SON-element so as to be notified of any accesses to properties.
 *
 * @author Alexander Kolosov
 */
public interface PropertyAccessListener extends EventListener {
    
    /**
     * This method gets called when a property is accessed.
     * 
     * @param evt   A PropertyAccessEvent object describing the source 
     *              and the property that has accessed
     */
    public void propertyAccess(PropertyAccessEvent evt);
}
