/*
 * GENERATED FILE. DO NOT MODIFY DIRECTLY!
 * See "templates" subdirectory.
 */

package ru.petrsu.nest.son.jpa;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import javax.persistence.*;

@Entity
@org.hibernate.annotations.AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class AbstractOU extends SonElement {

    @ManyToOne(cascade=CascadeType.ALL)
    private CompositeOU parent;

    public CompositeOU getParent() {
        firePropertyAccess("parent");

        return parent;
    }

    public void setParent(CompositeOU parent) {
        if (this.parent == parent)
            return;

        CompositeOU oldParent = this.parent;
        this.parent = parent;

        if (oldParent != null)
            oldParent.removeOU(this);

        if (parent != null)
            parent.addOU(this);

        propertyChange.firePropertyChange("parent", oldParent, parent);
    }
}
