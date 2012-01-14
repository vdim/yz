/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

/**
 * A "PropertyAccess" event gets delivered whenever a SON-element is accessed 
 * using get or set methods. A PropertyAccessEvent object is sent as an 
 * argument to the PropertyAccessListener method.
 *
 * @author Alexander Kolosov
 */
public class PropertyAccessEvent {
    private String propertyName;
    private Object source;
    
    public PropertyAccessEvent(Object source, String propertyName) {
        this.source = source;
        this.propertyName = propertyName;
    }
    
    public String getPropertyName() {
        return propertyName;
    }
    
    public Object getSource() {
        return source;
    }
}
