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
public class IPNetwork extends Network {

    private byte[] address;

    private byte[] mask;

    public IPNetwork() {
    }
  
    public IPNetwork(byte[] address, byte[] mask) {
        setAddress(address);
        setMask(mask);
    }
  
    public byte[] getAddress() {
        firePropertyAccess("address");

        return address;
    }

    public void setAddress(byte[] address) {
        if (Arrays.equals(this.address, address))
            return;

        byte[] oldAddress = this.address;
        this.address = address;

        propertyChange.firePropertyChange("address", oldAddress, address);
    }

    public byte[] getMask() {
        firePropertyAccess("mask");

        return mask;
    }

    public void setMask(byte[] mask) {
        if (Arrays.equals(this.mask, mask))
            return;

        byte[] oldMask = this.mask;
        this.mask = mask;

        propertyChange.firePropertyChange("mask", oldMask, mask);
    }


    @Override
    public String getDisplayName() {
        return String.valueOf(AddressUtils.addressOrName(this));
    }
}
