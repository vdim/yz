/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 *
 * @author Alexander Kolosov
 */
@Entity
@DiscriminatorValue("PROPERTY")
public class SonPropertyModificationOccurence extends AbstractSonModificationOccurence {
    private String oldValue;
    private String newValue;

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}
