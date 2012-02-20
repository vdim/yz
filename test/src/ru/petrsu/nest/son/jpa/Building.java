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
public class Building extends SonElement {

    private String address;

    @OneToMany(mappedBy="building", cascade=CascadeType.ALL)
    private Set<Floor> floors = new HashSet<Floor>();

    public String getAddress() {
        firePropertyAccess("address");

        return address;
    }

    public void setAddress(String address) {
        if (this.address == address)
            return;

        String oldAddress = this.address;
        this.address = address;

        propertyChange.firePropertyChange("address", oldAddress, address);
    }

    private class FloorsObserver implements CollectionObserver<Floor> {
        public void elementAdded(Floor e, Collection<? extends Floor> observable) {
            e.setBuilding(Building.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(Building.this, "floors", e));
        }     
    
        public void elementRemoved(Floor e, Collection<? extends Floor> observable) {
            e.setBuilding(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(Building.this, "floors", e));
        }
    }

    public Set<Floor> getFloors() {
        firePropertyAccess("floors");

        return ObservableCollections.observableSet(floors, new FloorsObserver());
    }

    public void setFloors(Set<Floor> floors) {
        if (this.floors == floors)
            return;

        ObservableCollections.unifySets(this.floors, floors, new FloorsObserver());
    }

    public void addFloor(Floor floor) {
        firePropertyAccess("floors");
        if (floors.add(floor)) {
            floor.setBuilding(Building.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "floors", floor));
        }
    }

    public void removeFloor(Floor floor) {
        firePropertyAccess("floors");
        if (floors.remove(floor)) {
            floor.setBuilding(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "floors", floor));
        }
    }
  
    @Override
    public String getIconName() {
        return "son-building.png";
    }
}
