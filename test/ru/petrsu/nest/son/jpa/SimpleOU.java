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
public class SimpleOU extends AbstractOU {

    @OneToMany(mappedBy="OU", cascade=CascadeType.ALL)
    private Set<Occupancy> occupancies = new HashSet<Occupancy>();

    private class OccupanciesObserver implements CollectionObserver<Occupancy> {
        public void elementAdded(Occupancy e, Collection<? extends Occupancy> observable) {
            e.setOU(SimpleOU.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(SimpleOU.this, "occupancies", e));
        }     
    
        public void elementRemoved(Occupancy e, Collection<? extends Occupancy> observable) {
            e.setOU(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(SimpleOU.this, "occupancies", e));
        }
    }

    public Set<Occupancy> getOccupancies() {
        firePropertyAccess("occupancies");

        return ObservableCollections.observableSet(occupancies, new OccupanciesObserver());
    }

    public void setOccupancies(Set<Occupancy> occupancies) {
        if (this.occupancies == occupancies)
            return;

        ObservableCollections.unifySets(this.occupancies, occupancies, new OccupanciesObserver());
    }

    public void addOccupancy(Occupancy occupancy) {
        firePropertyAccess("occupancies");
        if (occupancies.add(occupancy)) {
            occupancy.setOU(SimpleOU.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "occupancies", occupancy));
        }
    }

    public void removeOccupancy(Occupancy occupancy) {
        firePropertyAccess("occupancies");
        if (occupancies.remove(occupancy)) {
            occupancy.setOU(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "occupancies", occupancy));
        }
    }
  
    @Override
    public String getIconName() {
        return "son-ou-simple.png";
    }
}
