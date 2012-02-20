/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.FetchMode;

/**
 *
 * @author Alexander Kolosov
 */
@Entity
@DiscriminatorValue("REFERENCE")
public class SonReferenceModificationOccurence extends AbstractSonModificationOccurence {    
    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @org.hibernate.annotations.Fetch(FetchMode.SELECT)
    private SonElement oldReference;
    
    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @org.hibernate.annotations.Fetch(FetchMode.SELECT)
    private SonElement newReference;

    public SonElement getOldReference() {
        return oldReference;
    }

    public void setOldReference(SonElement oldReference) {
        this.oldReference = oldReference;
    }

    public SonElement getNewReference() {
        return newReference;
    }

    public void setNewReference(SonElement newReference) {
        this.newReference = newReference;
    }
    
}
