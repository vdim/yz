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
public class NetworkInterface extends SonElement {

    private byte[] inetAddress;

    private boolean active = true;

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private LinkInterface linkInterface;

    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Network network;

    public NetworkInterface() {
    }
  
    public NetworkInterface(byte[] inetAddress) {
        setInetAddress(inetAddress);
    }
  
    public NetworkInterface(LinkInterface linkInterface) {
        setLinkInterface(linkInterface);
    }
  
    public byte[] getInetAddress() {
        firePropertyAccess("inetAddress");

        return inetAddress;
    }

    public void setInetAddress(byte[] inetAddress) {
        if (Arrays.equals(this.inetAddress, inetAddress))
            return;

        byte[] oldInetAddress = this.inetAddress;
        this.inetAddress = inetAddress;

        propertyChange.firePropertyChange("inetAddress", oldInetAddress, inetAddress);
    }

    public boolean isActive() {
        firePropertyAccess("active");

        return active;
    }

    public void setActive(boolean active) {
        if (this.active == active)
            return;

        boolean oldActive = this.active;
        this.active = active;

        propertyChange.firePropertyChange("active", oldActive, active);
    }

    public LinkInterface getLinkInterface() {
        firePropertyAccess("linkInterface");

        return linkInterface;
    }

    public void setLinkInterface(LinkInterface linkInterface) {
        if (this.linkInterface == linkInterface)
            return;

        LinkInterface oldLinkInterface = this.linkInterface;
        this.linkInterface = linkInterface;

        if (oldLinkInterface != null)
            oldLinkInterface.removeNetworkInterface(this);

        if (linkInterface != null)
            linkInterface.addNetworkInterface(this);

        propertyChange.firePropertyChange("linkInterface", oldLinkInterface, linkInterface);
    }

    public Network getNetwork() {
        firePropertyAccess("network");

        return network;
    }

    public void setNetwork(Network network) {
        if (this.network == network)
            return;

        Network oldNetwork = this.network;
        this.network = network;

        if (oldNetwork != null)
            oldNetwork.removeNetworkInterface(this);

        if (network != null)
            network.addNetworkInterface(this);

        propertyChange.firePropertyChange("network", oldNetwork, network);
    }

    @Override
    public String getIconName() {
        return "son-interface.png";
    }
}
