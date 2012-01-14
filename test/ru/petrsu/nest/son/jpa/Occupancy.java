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
public class Occupancy extends SonElement {

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private SimpleOU OU;

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Room room;

    @OneToMany(mappedBy="occupancy", cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Set<Device> devices = new HashSet<Device>();

    public Occupancy() {
    }
  
    public Occupancy(Room room, SimpleOU OU) {
        setRoom(room);
        setOU(OU);
    }
  
    public SimpleOU getOU() {
        firePropertyAccess("OU");

        return OU;
    }

    public void setOU(SimpleOU OU) {
        if (this.OU == OU)
            return;

        SimpleOU oldOU = this.OU;
        this.OU = OU;

        if (oldOU != null)
            oldOU.removeOccupancy(this);

        if (OU != null)
            OU.addOccupancy(this);

        propertyChange.firePropertyChange("OU", oldOU, OU);
    }

    public Room getRoom() {
        firePropertyAccess("room");

        return room;
    }

    public void setRoom(Room room) {
        if (this.room == room)
            return;

        Room oldRoom = this.room;
        this.room = room;

        if (oldRoom != null)
            oldRoom.removeOccupancy(this);

        if (room != null)
            room.addOccupancy(this);

        propertyChange.firePropertyChange("room", oldRoom, room);
    }

    private class DevicesObserver implements CollectionObserver<Device> {
        public void elementAdded(Device e, Collection<? extends Device> observable) {
            e.setOccupancy(Occupancy.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(Occupancy.this, "devices", e));
        }     
    
        public void elementRemoved(Device e, Collection<? extends Device> observable) {
            e.setOccupancy(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(Occupancy.this, "devices", e));
        }
    }

    public Set<Device> getDevices() {
        firePropertyAccess("devices");

        return ObservableCollections.observableSet(devices, new DevicesObserver());
    }

    public void setDevices(Set<Device> devices) {
        if (this.devices == devices)
            return;

        ObservableCollections.unifySets(this.devices, devices, new DevicesObserver());
    }

    public void addDevice(Device device) {
        firePropertyAccess("devices");
        if (devices.add(device)) {
            device.setOccupancy(Occupancy.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "devices", device));
        }
    }

    public void removeDevice(Device device) {
        firePropertyAccess("devices");
        if (devices.remove(device)) {
            device.setOccupancy(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "devices", device));
        }
    }
  }
