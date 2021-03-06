/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 * @requires vm.jvmci
 * @library /test/lib /
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.vm.ci/jdk.vm.ci.hotspot
 *          jdk.vm.ci/jdk.vm.ci.code
 *
 * @build jdk.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper sun.hotspot.WhiteBox
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *                                sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   compiler.jvmci.compilerToVM.CanInlineMethodTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CanInlineMethodTest {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static void main(String[] args) {
        List<Executable> testCases = createTestCases();
        testCases.forEach(CanInlineMethodTest::runSanityTest);
    }

    private static void runSanityTest(Executable aMethod) {
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        boolean canInline = CompilerToVMHelper.canInlineMethod(method);
        boolean expectedCanInline = !WB.testSetDontInlineMethod(aMethod,
                true);
        Asserts.assertEQ(canInline, expectedCanInline, "Unexpected initial " +
                "value of property 'can inline'");

        canInline = CompilerToVMHelper.canInlineMethod(method);
        Asserts.assertFalse(canInline, aMethod + "Unexpected value of " +
                "property 'can inline' after setting 'do not inline' to true");
        WB.testSetDontInlineMethod(aMethod, false);
        canInline = CompilerToVMHelper.canInlineMethod(method);
        Asserts.assertTrue(canInline, "Unexpected value of " +
                "property 'can inline' after setting 'do not inline' to false");
    }

    private static List<Executable> createTestCases() {
        List<Executable> testCases = new ArrayList<>();

        Class<?> aClass = DummyClass.class;
        testCases.addAll(Arrays.asList(aClass.getDeclaredMethods()));
        testCases.addAll(Arrays.asList(aClass.getDeclaredConstructors()));
        return testCases;
    }
}
