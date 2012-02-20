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
public class Room extends SonElement {

    private String number;

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Floor floor;

    @OneToMany(mappedBy="room", cascade=CascadeType.ALL)
    private Set<Occupancy> occupancies = new HashSet<Occupancy>();

    public Room() {
    }
  
    public Room(String number) {
        setNumber(number);
    }
  
    public String getNumber() {
        firePropertyAccess("number");

        return number;
    }

    public void setNumber(String number) {
        if (this.number == number)
            return;

        String oldNumber = this.number;
        this.number = number;

        propertyChange.firePropertyChange("number", oldNumber, number);
    }

    public Floor getFloor() {
        firePropertyAccess("floor");

        return floor;
    }

    public void setFloor(Floor floor) {
        if (this.floor == floor)
            return;

        Floor oldFloor = this.floor;
        this.floor = floor;

        if (oldFloor != null)
            oldFloor.removeRoom(this);

        if (floor != null)
            floor.addRoom(this);

        propertyChange.firePropertyChange("floor", oldFloor, floor);
    }

    private class OccupanciesObserver implements CollectionObserver<Occupancy> {
        public void elementAdded(Occupancy e, Collection<? extends Occupancy> observable) {
            e.setRoom(Room.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(Room.this, "occupancies", e));
        }     
    
        public void elementRemoved(Occupancy e, Collection<? extends Occupancy> observable) {
            e.setRoom(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(Room.this, "occupancies", e));
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
            occupancy.setRoom(Room.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "occupancies", occupancy));
        }
    }

    public void removeOccupancy(Occupancy occupancy) {
        firePropertyAccess("occupancies");
        if (occupancies.remove(occupancy)) {
            occupancy.setRoom(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "occupancies", occupancy));
        }
    }
  
    @Override
    public String getIconName() {
        return "son-room.png";
    }


    @Override
    public String getDisplayName() {
        return String.valueOf(number);
    }
}
