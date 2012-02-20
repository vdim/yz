/*
 *  Copyright (C) 2011 Petrozavodsk State University
 *
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

/**
 *
 * @author Mihkail Kryshen
 */
class AddressUtils {

    private AddressUtils() {
    }

    static String briefDotDecimalString(byte[] address, byte[] mask,
            String alt) {

        if (address == null || mask == null || 
                address.length == 0 || 
                address.length != mask.length) {
            return alt;
        }

        if ((0xFF & mask[0]) != 0xFF)
            return Address.dotDecimalString(address);

        StringBuilder sb = new StringBuilder(4 * 3);

        int i = 1;
        while (i < address.length && (0xFF & mask[i]) == 0xFF) {
            i++;
        }

        while (i < address.length) {
            sb.append('.');
            sb.append(Integer.toString(0xFF & address[i]));
            i++;
        }

        return sb.toString();
    }

    static String addressOrName(IPv4Interface in) {
        byte[] address = in.getInetAddress();

        if (address == null)
            return in.getName();

        return Address.dotDecimalString(address);
    }

    static String addressOrName(IPNetwork n) {
        byte[] address = n.getAddress();

        if (address == null) {
            return n.getName();
        }

        return Address.dotDecimalString(address);
    }

    static String briefAddressOrName(IPv4Interface in) {
        Network net = in.getNetwork();

        if (!(net instanceof IPNetwork)) {
            return addressOrName(in);
        }

        return briefDotDecimalString(in.getInetAddress(),
                ((IPNetwork) net).getMask(),
                in.getName());
    }

    static String nameOrAddress(EthernetInterface eth) {
        String name = eth.getName();

        if (name != null && !name.isEmpty())
            return name;

        byte[] addr = eth.getMACAddress();

        if (addr == null)
            return name;

        return Address.IEEE802String(addr);
    }
}
