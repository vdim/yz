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
public class SON extends SonElement {

    @OneToOne(cascade=CascadeType.ALL)
    private Device rootDevice;

    @OneToOne(cascade=CascadeType.ALL)
    private CompositeOU rootOU;

    @OneToMany(cascade=CascadeType.ALL)
    private Set<Building> buildings = new HashSet<Building>();

    @OneToMany(cascade=CascadeType.ALL)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SELECT)
    @OrderBy("date")
    private List<AbstractSonModificationOccurence> modificationOccurences = new ArrayList<AbstractSonModificationOccurence>();

    public Device getRootDevice() {
        firePropertyAccess("rootDevice");

        return rootDevice;
    }

    public void setRootDevice(Device rootDevice) {
        if (this.rootDevice == rootDevice)
            return;

        Device oldRootDevice = this.rootDevice;
        this.rootDevice = rootDevice;

        propertyChange.firePropertyChange("rootDevice", oldRootDevice, rootDevice);
    }

    public CompositeOU getRootOU() {
        firePropertyAccess("rootOU");

        return rootOU;
    }

    public void setRootOU(CompositeOU rootOU) {
        if (this.rootOU == rootOU)
            return;

        CompositeOU oldRootOU = this.rootOU;
        this.rootOU = rootOU;

        propertyChange.firePropertyChange("rootOU", oldRootOU, rootOU);
    }

    private class BuildingsObserver implements CollectionObserver<Building> {
        public void elementAdded(Building e, Collection<? extends Building> observable) {
            propertyChange.firePropertyChange(new PropertyValueAddEvent(SON.this, "buildings", e));
        }     
    
        public void elementRemoved(Building e, Collection<? extends Building> observable) {
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(SON.this, "buildings", e));
        }
    }

    public Set<Building> getBuildings() {
        firePropertyAccess("buildings");

        return ObservableCollections.observableSet(buildings, new BuildingsObserver());
    }

    public void setBuildings(Set<Building> buildings) {
        if (this.buildings == buildings)
            return;

        ObservableCollections.unifySets(this.buildings, buildings, new BuildingsObserver());
    }

    public void addBuilding(Building building) {
        firePropertyAccess("buildings");
        if (buildings.add(building)) {
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "buildings", building));
        }
    }

    public void removeBuilding(Building building) {
        firePropertyAccess("buildings");
        if (buildings.remove(building)) {
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "buildings", building));
        }
    }
  
    public List<AbstractSonModificationOccurence> getModificationOccurences() {
        firePropertyAccess("modificationOccurences");

        return modificationOccurences;
    }

    public void setModificationOccurences(List<AbstractSonModificationOccurence> modificationOccurences) {
        if (this.modificationOccurences == modificationOccurences)
            return;

        List<AbstractSonModificationOccurence> oldModificationOccurences = this.modificationOccurences;
        this.modificationOccurences = modificationOccurences;

        propertyChange.firePropertyChange("modificationOccurences", oldModificationOccurences, modificationOccurences);
    }
}
