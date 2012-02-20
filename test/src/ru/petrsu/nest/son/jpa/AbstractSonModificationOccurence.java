/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.FetchMode;

/**
 * This class represents an abstract occurence of any SonElement modification.
 * 
 * @author Alexander Kolosov
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class AbstractSonModificationOccurence {
    @Id
    @GeneratedValue
    @Column(name="beanId")
    private Long id;
    
    protected Date date;
    
    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @org.hibernate.annotations.Fetch(FetchMode.SELECT)
    protected SonElement element;
    protected String propertyName;
    
    public Long getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public SonElement getElement() {
        return element;
    }

    public void setElement(SonElement element) {
        this.element = element;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }        
}
