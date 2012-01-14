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
public class Network extends SonElement {

    @OneToMany(mappedBy="network", cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private Set<NetworkInterface> networkInterfaces = new HashSet<NetworkInterface>();

    private class NetworkInterfacesObserver implements CollectionObserver<NetworkInterface> {
        public void elementAdded(NetworkInterface e, Collection<? extends NetworkInterface> observable) {
            e.setNetwork(Network.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(Network.this, "networkInterfaces", e));
        }     
    
        public void elementRemoved(NetworkInterface e, Collection<? extends NetworkInterface> observable) {
            e.setNetwork(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(Network.this, "networkInterfaces", e));
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
            networkInterface.setNetwork(Network.this);
            propertyChange.firePropertyChange(new PropertyValueAddEvent(this, "networkInterfaces", networkInterface));
        }
    }

    public void removeNetworkInterface(NetworkInterface networkInterface) {
        firePropertyAccess("networkInterfaces");
        if (networkInterfaces.remove(networkInterface)) {
            networkInterface.setNetwork(null);
            propertyChange.firePropertyChange(new PropertyValueRemoveEvent(this, "networkInterfaces", networkInterface));
        }
    }
  
    @Override
    public String getIconName() {
        return "son-network.png";
    }
}
