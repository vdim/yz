/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.beans.PropertyChangeEvent;

/**
 * Delivered when a value is removed from a multi-value property.
 *
 * @author Mikhail Kryshen
 */
public class PropertyValueRemoveEvent extends PropertyChangeEvent {
    private Object removed;
    
    public PropertyValueRemoveEvent
            (Object source, String propertyName, Object value) {
        
        super(source, propertyName, null, null);
        
        this.removed = value;
    }
    
    public Object getRemovedValue() {
        return removed;
    }
}
