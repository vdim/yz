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
public class Device extends SonElement {

    private boolean forwarding = false;

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Occupancy occupancy;

    @OneToMany(mappedBy="device", cascade=CascadeType.ALL)
    private Set<LinkInterface> linkInterfaces = new HashSet<LinkInterface>();

    public boolean isForwarding() {
        firePropertyAccess("forwarding");

        return forwarding;
    }

    public void setForwarding(boolean forwarding) {
        if (this.forwarding == forwarding)
            return;

        boolean oldForwarding = this.forwarding;
        this.forwarding = forwarding;

        propertyChange.firePropertyChange("forwarding", oldForwarding, forwarding);
    }

    public Occupancy getOccupancy() {
        firePropertyAccess("occupancy");

        return occupancy;
    }

    public void setOccupancy(Occupancy occupancy) {
        if (this.occupancy == occupancy)
            return;

        Occupancy oldOccupancy = this.occupancy;
        this.occupancy = occupancy;

        if (oldOccupancy != null)
            oldOccupancy.removeDevice(this);

        if (occupancy != null)
            occupancy.addDevice(this);

        propertyChange.firePropertyChange("occupancy", oldOccupancy, occupancy);
    }

    private class LinkInterfacesObserver implements CollectionObserver<LinkInterface> {
        public void elementAdded(LinkInterface e, Collection<? extends LinkInterface> observable) {
            e.setDevice(Device.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(Device.this, "linkInterfaces", e));
        }     
    
        public void elementRemoved(LinkInterface e, Collection<? extends LinkInterface> observable) {
            e.setDevice(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(Device.this, "linkInterfaces", e));
        }
    }

    public Set<LinkInterface> getLinkInterfaces() {
        firePropertyAccess("linkInterfaces");

        return ObservableCollections.observableSet(linkInterfaces, new LinkInterfacesObserver());
    }

    public void setLinkInterfaces(Set<LinkInterface> linkInterfaces) {
        if (this.linkInterfaces == linkInterfaces)
            return;

        ObservableCollections.unifySets(this.linkInterfaces, linkInterfaces, new LinkInterfacesObserver());
    }

    public void addLinkInterface(LinkInterface linkInterface) {
        firePropertyAccess("linkInterfaces");
        if (linkInterfaces.add(linkInterface)) {
            linkInterface.setDevice(Device.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "linkInterfaces", linkInterface));
        }
    }

    public void removeLinkInterface(LinkInterface linkInterface) {
        firePropertyAccess("linkInterfaces");
        if (linkInterfaces.remove(linkInterface)) {
            linkInterface.setDevice(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "linkInterfaces", linkInterface));
        }
    }
  
    @Override
    public String getIconName() {
        return isForwarding() ? "son-device-router.png" : "son-device.png";
    }
}
