/*
 * Copyright 2011 Vyacheslav Dimitrov <vyacheslav.dimitrov@gmail.com>
 *
 * This file is part of YZ.
 *
 * YZ is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * YZ is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YZ.  If not, see
 * <http://www.gnu.org/licenses/>.
*/

package ru.petrsu.nest.yz;

import java.util.StringTokenizer;

/**
 * Class with some helper method.
 *
 * @author Vyacheslav Dimitrov.
 */ 
public class YZUtils {

    private YZUtils() {
    }

    /**
     * Gets string with MAC address (like "") and returns
     * array of its bytes.
     * 
     * Motivation for this method was trouble that
     * clojure can not correct coerce unsigned byte (greather than 127) to
     * byte, but Java can (value[n] = (byte)Integer.parseInt(s, 16)).
     *
     */ 
    public static byte[] getMACfromString(String string) {
	StringTokenizer st = new StringTokenizer(string, ":");
	byte[] value = new byte[st.countTokens()];

	for (int n = 0; st.hasMoreTokens(); n++) {
	    String s = st.nextToken();
	    value[n] = (byte)Integer.parseInt(s, 16);
	}

	return value;
    }
}
