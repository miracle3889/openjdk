/*
 * Copyright 2003 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @bug 4856968
 * @summary make sure add/insert/removeProvider() work correctly
 * @author Andreas Sterbenz
 */

import java.util.*;

import java.security.*;

public class ChangeProviders extends Provider {

    private ChangeProviders() {
        super("Foo", 47.23d, "none");
    }

    private static int plen() {
        return Security.getProviders().length;
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        Provider p = new ChangeProviders();

        int n = plen();
        Security.addProvider(p);
        if (plen() != n + 1) {
            throw new Exception("Provider not added");
        }
        Security.addProvider(p);
        if (plen() != n + 1) {
            throw new Exception("Provider readded");
        }
        Security.insertProviderAt(p, 1);
        if (plen() != n + 1) {
            throw new Exception("Provider readded");
        }
        Security.removeProvider(p.getName());
        if ((plen() != n) || (Security.getProvider(p.getName()) != null)) {
            throw new Exception("Provider not removed");
        }
        Security.insertProviderAt(p, 1);
        if (plen() != n + 1) {
            throw new Exception("Provider not added");
        }
        if (Security.getProviders()[0] != p) {
            throw new Exception("Provider not at pos 1");
        }

        System.out.println("All tests passed.");
    }

}
