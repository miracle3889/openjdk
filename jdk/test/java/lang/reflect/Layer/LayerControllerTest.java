/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @library /lib/testlibrary
 * @build LayerControllerTest ModuleUtils
 * @run testng LayerControllerTest
 * @summary Basic tests for java.lang.reflect.Layer.Controller
 */

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Layer;
import java.lang.reflect.Module;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class LayerControllerTest {

    /**
     * Creates a Controller for a module layer containing modules m1 and m2.
     * Module m1 contains p1, reads java.base, does not export/open any package
     * Module m2 contains p2, reads java.base, does not export/open any package
     */
    private Layer.Controller createTestLayer() {
        ModuleDescriptor descriptor1
            = ModuleDescriptor.module("m1")
                .contains("p1")
                .requires("java.base")
                .build();

        ModuleDescriptor descriptor2
            = ModuleDescriptor.module("m2")
                .requires("java.base")
                .contains("p2")
                .build();

        ModuleFinder finder = ModuleUtils.finderOf(descriptor1, descriptor2);
        Layer bootLayer = Layer.boot();

        Configuration cf = bootLayer.configuration()
                .resolveRequires(finder, ModuleFinder.of(), Set.of("m1", "m2"));

        ClassLoader scl = ClassLoader.getSystemClassLoader();

        Layer.Controller controller
                = Layer.defineModulesWithOneLoader(cf, List.of(bootLayer), scl);

        Layer layer = controller.layer();

        assertTrue(layer.modules().size() == 2);
        assertTrue(layer.findModule("m1").isPresent());
        assertTrue(layer.findModule("m2").isPresent());

        return controller;
    }

    /**
     * Basic test of Layer.Controller to update modules m1 and m2 to read and
     * open packages to each other.
     */
    public void testBasic() {
        Layer.Controller controller = createTestLayer();
        Layer layer = controller.layer();
        Module m1 = layer.findModule("m1").orElseThrow(RuntimeException::new);
        Module m2 = layer.findModule("m2").orElseThrow(RuntimeException::new);

        assertFalse(m1.canRead(m2));
        assertFalse(m1.isExported("p1"));
        assertFalse(m1.isOpen("p1"));
        assertFalse(m1.isExported("p1", m2));
        assertFalse(m1.isOpen("p1", m2));

        assertFalse(m2.canRead(m1));
        assertFalse(m2.isExported("p2"));
        assertFalse(m2.isOpen("p2"));
        assertFalse(m2.isExported("p2", m1));
        assertFalse(m2.isOpen("p2", m1));

        // update m1 to read m2
        assertTrue(controller.addReads(m1, m2) == controller);
        assertTrue(m1.canRead(m2));
        assertFalse(m2.canRead(m1));

        // update m2 to read m1
        assertTrue(controller.addReads(m2, m1) == controller);
        assertTrue(m1.canRead(m2));
        assertTrue(m1.canRead(m1));

        // update m1 to open p1 to m2
        assertTrue(controller.addOpens(m1, "p1", m2) == controller);
        assertTrue(m1.isExported("p1", m2));
        assertTrue(m1.isOpen("p1", m2));
        assertFalse(m1.isExported("p1"));
        assertFalse(m1.isOpen("p1"));

        // update m2 to open p2 to m1
        assertTrue(controller.addOpens(m2, "p2", m1) == controller);
        assertTrue(m2.isExported("p2", m1));
        assertTrue(m2.isOpen("p2", m1));
        assertFalse(m2.isExported("p2"));
        assertFalse(m2.isOpen("p2"));
    }

    /**
     * Test invalid argument handling
     */
    public void testBadArguments() {
        Layer.Controller controller = createTestLayer();
        Layer layer = controller.layer();
        Module m1 = layer.findModule("m1").orElseThrow(RuntimeException::new);
        Module m2 = layer.findModule("m2").orElseThrow(RuntimeException::new);
        Module base = Object.class.getModule();

        // java.base is not in layer
        try {
            controller.addReads(base, m2);
            assertTrue(false);
        } catch (IllegalArgumentException expected) { }

        // java.base is not in layer
        try {
            controller.addOpens(base, "java.lang", m2);
            assertTrue(false);
        } catch (IllegalArgumentException expected) { }

        // m1 does not contain java.lang
        try {
            controller.addOpens(m1, "java.lang", m2);
            assertTrue(false);
        } catch (IllegalArgumentException expected) { }
    }

    /**
     * Test null handling
     */
    public void testNulls() {
        Layer.Controller controller = createTestLayer();
        Layer layer = controller.layer();
        Module m1 = layer.findModule("m1").orElseThrow(RuntimeException::new);
        Module m2 = layer.findModule("m2").orElseThrow(RuntimeException::new);
        assertTrue(m1 != null);
        assertTrue(m2 != null);

        try {
            controller.addReads(null, m2);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        try {
            controller.addReads(m1, null);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        try {
            controller.addOpens(null, "p1", m2);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        try {
            controller.addOpens(m1, null, m2);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        try {
            controller.addOpens(m1, "p1", null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
    }
}