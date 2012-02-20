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
public class CompositeOU extends AbstractOU {

    @OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
    private Set<AbstractOU> OUs = new HashSet<AbstractOU>();

    private class OUsObserver implements CollectionObserver<AbstractOU> {
        public void elementAdded(AbstractOU e, Collection<? extends AbstractOU> observable) {
            e.setParent(CompositeOU.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(CompositeOU.this, "OUs", e));
        }     
    
        public void elementRemoved(AbstractOU e, Collection<? extends AbstractOU> observable) {
            e.setParent(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(CompositeOU.this, "OUs", e));
        }
    }

    public Set<AbstractOU> getOUs() {
        firePropertyAccess("OUs");

        return ObservableCollections.observableSet(OUs, new OUsObserver());
    }

    public void setOUs(Set<AbstractOU> OUs) {
        if (this.OUs == OUs)
            return;

        ObservableCollections.unifySets(this.OUs, OUs, new OUsObserver());
    }

    public void addOU(AbstractOU OU) {
        firePropertyAccess("OUs");
        if (OUs.add(OU)) {
            OU.setParent(CompositeOU.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "OUs", OU));
        }
    }

    public void removeOU(AbstractOU OU) {
        firePropertyAccess("OUs");
        if (OUs.remove(OU)) {
            OU.setParent(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "OUs", OU));
        }
    }
  
    @Override
    public String getIconName() {
        return "son-ou-composite.png";
    }
}
