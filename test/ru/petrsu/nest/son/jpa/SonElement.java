/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;
import javax.swing.event.EventListenerList;

/**
 * Basic SON structure element. Provides common methods for SON beans.
 *
 * @author Mikhail Kryshen
 * @author Alexander Kolosov
 */
@Entity
@org.hibernate.annotations.AccessType("field")
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class SonElement implements Serializable {
    @Id
    @GeneratedValue
    @Column(name="beanId")
    protected Long id;

    @Column(name="UUID", unique=true, nullable=false, updatable=false, length = 36)
    // String representation of UUID to store it in the DB
    private String internalUUID;

    @Transient
    // The object, that represents the UUID
    private UUID realUUID;

    
    @Transient
    transient private EventListenerList listenerList = new EventListenerList();
    
    @Transient
    transient protected PropertyChangeSupport propertyChange
            = new PropertyChangeSupport(this);
    
    private String description;    
    private String name;
    
    public SonElement() {
        this.internalUUID = java.util.UUID.randomUUID().toString();
    }
    
    public String getDescription() {
        firePropertyAccess("description");
        return description;
    }
    
    public void setDescription(String description) {
        if (this.description == description)
            return;

        String oldDescription = this.description;
        this.description = description;
        propertyChange.firePropertyChange("description", oldDescription, description);
    }
    
    public String getName() {
        firePropertyAccess("name");
        return name;                
    }
    
    public void setName(String name) {
        if (this.name == name)
            return;

        String oldName = this.name;
        this.name = name;
        propertyChange.firePropertyChange("name", oldName, name);
    }
    
    public void addPropertyAccessListener(PropertyAccessListener l) {
        listenerList.add(PropertyAccessListener.class, l);
    }
    
    public void removePropertyAccessListener(PropertyAccessListener l) {
        listenerList.remove(PropertyAccessListener.class, l);
    }        
    
    protected void firePropertyAccess(String propertyName) {
        PropertyAccessEvent event = 
                new PropertyAccessEvent(this, propertyName);
        
        for (PropertyAccessListener l : 
            listenerList.getListeners(PropertyAccessListener.class)) {
                        
            l.propertyAccess(event);
        }
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChange.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChange.removePropertyChangeListener(l);
    }
    
    public String getIconName() {
        return null;
    }
    
    /**
     * Returns the string which could be used 
     * to represent this element in the UI.
     */    
    public String getDisplayName() {
        return getName();
    }

    /**
     *
     */
    private UUID getRealUUID() {
        if (realUUID == null) {
            realUUID = UUID.fromString(internalUUID);
        }

        return realUUID;
    }
    
    /**
     * Returns the string representation of this element based on
     * the values returned by and <code>getDisplayName()</code>.
     *
     * @see #getDisplayName()
     */
    @Override
    public String toString() {
        String name = getDisplayName();
       
        if (name == null || name.length() == 0)
            return "";
        
        return name;
    }

    /**
     * Returns a brief string representation of this element.
     */
    public String briefString() {
        return getDisplayName();
    }

    @Override
    public int hashCode() {
        return getRealUUID().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if ( !(o instanceof SonElement) )
            return false;

        if ( (id == null) || (((SonElement)o).id == null) )
            return getRealUUID().equals( ((SonElement)o).getRealUUID() );

        return id.equals( ((SonElement)o).id );
    }

    public Long getId () {
        return this.id;
    }
}
