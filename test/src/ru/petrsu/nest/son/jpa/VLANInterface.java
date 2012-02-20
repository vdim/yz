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
public class VLANInterface extends LinkInterface {

    private int vlanId;

    @ManyToMany(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Set<LinkInterface> linkInterfaces = new HashSet<LinkInterface>();

    public VLANInterface() {
    }
  
    public VLANInterface(int vlanId) {
        setVlanId(vlanId);
    }
  
    public VLANInterface(Device device) {
        setDevice(device);
    }
  
    public VLANInterface(Device device, int vlanId) {
        setDevice(device);
        setVlanId(vlanId);
    }
  
    public int getVlanId() {
        firePropertyAccess("vlanId");

        return vlanId;
    }

    public void setVlanId(int vlanId) {
        if (this.vlanId == vlanId)
            return;

        int oldVlanId = this.vlanId;
        this.vlanId = vlanId;

        propertyChange.firePropertyChange("vlanId", oldVlanId, vlanId);
    }

    private class LinkInterfacesObserver implements CollectionObserver<LinkInterface> {
        public void elementAdded(LinkInterface e, Collection<? extends LinkInterface> observable) {
            propertyChange.firePropertyChange(new PropertyValueAddEvent(VLANInterface.this, "linkInterfaces", e));
        }     
    
        public void elementRemoved(LinkInterface e, Collection<? extends LinkInterface> observable) {
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(VLANInterface.this, "linkInterfaces", e));
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
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "linkInterfaces", linkInterface));
        }
    }

    public void removeLinkInterface(LinkInterface linkInterface) {
        firePropertyAccess("linkInterfaces");
        if (linkInterfaces.remove(linkInterface)) {
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "linkInterfaces", linkInterface));
        }
    }
  }
