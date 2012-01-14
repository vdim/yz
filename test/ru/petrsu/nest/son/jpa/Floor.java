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
public class Floor extends SonElement {

    private int number;

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Building building;

    @OneToMany(mappedBy="floor", cascade=CascadeType.ALL)
    private Set<Room> rooms = new HashSet<Room>();

    public Floor() {
    }
  
    public Floor(int number) {
        setNumber(number);
    }
  
    public int getNumber() {
        firePropertyAccess("number");

        return number;
    }

    public void setNumber(int number) {
        if (this.number == number)
            return;

        int oldNumber = this.number;
        this.number = number;

        propertyChange.firePropertyChange("number", oldNumber, number);
    }

    public Building getBuilding() {
        firePropertyAccess("building");

        return building;
    }

    public void setBuilding(Building building) {
        if (this.building == building)
            return;

        Building oldBuilding = this.building;
        this.building = building;

        if (oldBuilding != null)
            oldBuilding.removeFloor(this);

        if (building != null)
            building.addFloor(this);

        propertyChange.firePropertyChange("building", oldBuilding, building);
    }

    private class RoomsObserver implements CollectionObserver<Room> {
        public void elementAdded(Room e, Collection<? extends Room> observable) {
            e.setFloor(Floor.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(Floor.this, "rooms", e));
        }     
    
        public void elementRemoved(Room e, Collection<? extends Room> observable) {
            e.setFloor(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(Floor.this, "rooms", e));
        }
    }

    public Set<Room> getRooms() {
        firePropertyAccess("rooms");

        return ObservableCollections.observableSet(rooms, new RoomsObserver());
    }

    public void setRooms(Set<Room> rooms) {
        if (this.rooms == rooms)
            return;

        ObservableCollections.unifySets(this.rooms, rooms, new RoomsObserver());
    }

    public void addRoom(Room room) {
        firePropertyAccess("rooms");
        if (rooms.add(room)) {
            room.setFloor(Floor.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "rooms", room));
        }
    }

    public void removeRoom(Room room) {
        firePropertyAccess("rooms");
        if (rooms.remove(room)) {
            room.setFloor(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "rooms", room));
        }
    }
  
    @Override
    public String getIconName() {
        return "son-floor.png";
    }


    @Override
    public String getDisplayName() {
        return String.valueOf(number);
    }
}
