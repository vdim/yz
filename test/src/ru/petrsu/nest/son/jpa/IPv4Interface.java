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
public class IPv4Interface extends NetworkInterface {

    public IPv4Interface() {
    }
  
    public IPv4Interface(byte[] inetAddress) {
        setInetAddress(inetAddress);
    }
  

    @Override
    public String getDisplayName() {
        return String.valueOf(AddressUtils.addressOrName(this));
    }


    @Override
    public String briefString() {
        return String.valueOf(AddressUtils.briefAddressOrName(this));
    }
}
