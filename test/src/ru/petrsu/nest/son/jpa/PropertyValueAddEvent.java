/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.beans.PropertyChangeEvent;

/**
 * Delivered when a value is added to a multi-value property.
 *
 * @author Mikhail Kryshen
 */
public class PropertyValueAddEvent extends PropertyChangeEvent {
    private Object added;
    
    public PropertyValueAddEvent
            (Object source, String propertyName, Object value) {
        
        super(source, propertyName, null, null);
        
        this.added = value;
    }
    
    public Object getAddedValue() {
        return added;
    }
}
