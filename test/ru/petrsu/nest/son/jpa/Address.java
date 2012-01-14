/*
 *  Copyright (C) 2011 Petrozavodsk State University
 *
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * This class represents a network address (IPv4, IPv6, MAC) of a device.
 *
 * The class stores the byte sequence and the type, describing the address. The
 * type is specified by <code>Type</code> enum and can be on of: <code>IPV4</code>,
 * <code>IPV6</code>, <code>MAC</code> and <code>UNSPECIFIED</code>.
 *  
 * @author Alexander Kolosov
 */
public final class Address {
    public enum Type {
        IPV4,
        IPV6,
        MAC,
        UNSPECIFIED
    }

    private final byte[] address;
    private final Type type;

    /**
     * Create the Address instance using given byte array with
     * <code>UNSPECIFIED</code> type.
     *
     * @param address byte array, describing the address
     */
    public Address(byte[] address) {
        this(address, Type.UNSPECIFIED);
    }

    /**
     * Create the Address instance using the <code>InetAddress</code> instance.
     * The type is determined using the actual class of the given
     * <code>InetAddress</code> instance: IPV4 in case of <code>Inet4Address</code>,
     * IPV6 in case of <code>Inet6Address</code>.
     *
     * @param address - InetAddress instance, representing the address
     */
    public Address(InetAddress address) {
        this(address.getAddress(), address instanceof Inet4Address ? Type.IPV4 : Type.IPV6);
    }

    /**
     * Create the Address instance using given byte sequence and type.
     *
     * @param address byte array, describing the address
     * @param type type of the address
     */
    public Address(byte[] address, Type type) {
        this.address = address;
        this.type = type;
    }

    public byte[] getBytes() {
        return address;
    }

    public Type getType() {
        return type;
    }

    public String toDotDecimalString() {
        return dotDecimalString(address);
    }

    public String toIEEE802String() {
        return IEEE802String(address);
    }

    public static String dotDecimalString(byte[] addr) {
        return formatAddress(addr, '.', "%d");
    }

    public static String IEEE802String(byte[] addr) {
        return formatAddress(addr, ':', "%02x");
    }

    public static String formatAddress(byte[] addr, char delim, String format) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < addr.length; i++) {
            int b = 0xFF & addr[i];
            sb.append(String.format(format, b));
            if (i < addr.length - 1) {
                sb.append(delim);
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Address other = (Address) obj;

        return Arrays.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    @Override
    public String toString() {
        switch(type) {
            case IPV4:
                return toDotDecimalString();

            case MAC:
                return toIEEE802String();

            default:
                return formatAddress(address, ',', "%x");
        }
    }
}
