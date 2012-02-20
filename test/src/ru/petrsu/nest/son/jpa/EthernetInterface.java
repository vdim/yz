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
public class EthernetInterface extends LinkInterface {

    private byte[] MACAddress;

    public EthernetInterface() {
    }
  
    public EthernetInterface(byte[] MACAddress) {
        setMACAddress(MACAddress);
    }
  
    public EthernetInterface(Device device) {
        setDevice(device);
    }
  
    public EthernetInterface(Device device, byte[] MACAddress) {
        setDevice(device);
        setMACAddress(MACAddress);
    }
  
    public byte[] getMACAddress() {
        firePropertyAccess("MACAddress");

        return MACAddress;
    }

    public void setMACAddress(byte[] MACAddress) {
        if (Arrays.equals(this.MACAddress, MACAddress))
            return;

        byte[] oldMACAddress = this.MACAddress;
        this.MACAddress = MACAddress;

        propertyChange.firePropertyChange("MACAddress", oldMACAddress, MACAddress);
    }


    @Override
    public String getDisplayName() {
        return String.valueOf(AddressUtils.nameOrAddress(this));
    }
}
