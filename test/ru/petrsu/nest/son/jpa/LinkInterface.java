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


/**
 * Link-level network interface.
 */
@Entity
@org.hibernate.annotations.AccessType("field")
public class LinkInterface extends SonElement {

    public static enum State {
        UP, DOWN, ADMIN_DOWN
    }
  
    public static enum Mode {
        DUPLEX, RECEPTION, TRANSMISSION
    }
  
    private State state = State.UP;

    private Mode mode = Mode.DUPLEX;

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Device device;

    @OneToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private LinkInterface link;

    @OneToMany(mappedBy="linkInterface", cascade=CascadeType.ALL)
    private Set<NetworkInterface> networkInterfaces = new HashSet<NetworkInterface>();

    public LinkInterface() {
    }
  
    public LinkInterface(Device device) {
        setDevice(device);
    }
  
    public State getState() {
        firePropertyAccess("state");

        return state;
    }

    public void setState(State state) {
        if (this.state == state)
            return;

        State oldState = this.state;
        this.state = state;

        propertyChange.firePropertyChange("state", oldState, state);
    }

    public Mode getMode() {
        firePropertyAccess("mode");

        return mode;
    }

    public void setMode(Mode mode) {
        if (this.mode == mode)
            return;

        Mode oldMode = this.mode;
        this.mode = mode;

        propertyChange.firePropertyChange("mode", oldMode, mode);
    }

    public Device getDevice() {
        firePropertyAccess("device");

        return device;
    }

    public void setDevice(Device device) {
        if (this.device == device)
            return;

        Device oldDevice = this.device;
        this.device = device;

        if (oldDevice != null)
            oldDevice.removeLinkInterface(this);

        if (device != null)
            device.addLinkInterface(this);

        propertyChange.firePropertyChange("device", oldDevice, device);
    }

    public LinkInterface getLink() {
        firePropertyAccess("link");

        return link;
    }

    public void setLink(LinkInterface link) {
        if (this.link == link)
            return;

        LinkInterface oldLink = this.link;
        this.link = link;

        propertyChange.firePropertyChange("link", oldLink, link);
    }

    private class NetworkInterfacesObserver implements CollectionObserver<NetworkInterface> {
        public void elementAdded(NetworkInterface e, Collection<? extends NetworkInterface> observable) {
            e.setLinkInterface(LinkInterface.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(LinkInterface.this, "networkInterfaces", e));
        }     
    
        public void elementRemoved(NetworkInterface e, Collection<? extends NetworkInterface> observable) {
            e.setLinkInterface(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(LinkInterface.this, "networkInterfaces", e));
        }
    }

    public Set<NetworkInterface> getNetworkInterfaces() {
        firePropertyAccess("networkInterfaces");

        return ObservableCollections.observableSet(networkInterfaces, new NetworkInterfacesObserver());
    }

    public void setNetworkInterfaces(Set<NetworkInterface> networkInterfaces) {
        if (this.networkInterfaces == networkInterfaces)
            return;

        ObservableCollections.unifySets(this.networkInterfaces, networkInterfaces, new NetworkInterfacesObserver());
    }

    public void addNetworkInterface(NetworkInterface networkInterface) {
        firePropertyAccess("networkInterfaces");
        if (networkInterfaces.add(networkInterface)) {
            networkInterface.setLinkInterface(LinkInterface.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "networkInterfaces", networkInterface));
        }
    }

    public void removeNetworkInterface(NetworkInterface networkInterface) {
        firePropertyAccess("networkInterfaces");
        if (networkInterfaces.remove(networkInterface)) {
            networkInterface.setLinkInterface(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "networkInterfaces", networkInterface));
        }
    }
  
    @Override
    public String getIconName() {
        return "son-interface-link.png";
    }
}
