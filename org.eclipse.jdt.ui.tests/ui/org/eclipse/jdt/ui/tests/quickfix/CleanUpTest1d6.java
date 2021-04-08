/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java1d6ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * Tests the cleanup features related to Java 6 (i.e. Mustang).
 */
public class CleanUpTest1d6 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java1d6ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	/**
	 * Tests if CleanUp works when the number of problems in a single CU is greater than the
	 * Compiler option {@link JavaCore#COMPILER_PB_MAX_PER_UNIT} which has a default value of 100,
	 * see http://bugs.eclipse.org/322543 for details.
	 *
	 * @throws Exception if the something fails while executing this test
	 * @since 3.7
	 */
	@Test
	public void testCleanUpWithCUProblemsGreaterThanMaxProblemsPerCUPreference() throws Exception {
		final int PROBLEMS_COUNT= 101;

		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("interface I {\n");

		for (int count= 0; count < PROBLEMS_COUNT; count++) {
			bld.append("    void m").append(count).append("();\n");
		}

		bld.append("}\n");
		bld.append("class X implements I {\n");

		for (int count= 0; count < PROBLEMS_COUNT; count++) {
			bld.append("    public void m").append(count).append("() {} // @Override error in 1.5, not in 1.6\n");
		}

		bld.append("}\n");
		String given= bld.toString();

		ICompilationUnit cu= pack.createCompilationUnit("I.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("interface I {\n");

		for (int count= 0; count < PROBLEMS_COUNT; count++) {
			bld.append("    void m").append(count).append("();\n");
		}

		bld.append("}\n");
		bld.append("class X implements I {\n");

		for (int count= 0; count < PROBLEMS_COUNT; count++) {
			bld.append("    @Override\n");
			bld.append("    public void m").append(count).append("() {} // @Override error in 1.5, not in 1.6\n");
		}

		bld.append("}\n");
		String expected= bld.toString();

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected }, null);
	}

	@Test
	public void testAddOverride1d6() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "interface I {\n" //
				+ "    void m();\n" //
				+ "    boolean equals(Object obj);\n" //
				+ "}\n" //
				+ "\n" //
				+ "interface J extends I {\n" //
				+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
				+ "}\n" //
				+ "\n" //
				+ "class X implements J {\n" //
				+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
				+ "    public int hashCode() { return 0; }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("I.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "interface I {\n" //
				+ "    void m();\n" //
				+ "    @Override\n" //
				+ "    boolean equals(Object obj);\n" //
				+ "}\n" //
				+ "\n" //
				+ "interface J extends I {\n" //
				+ "    @Override\n" //
				+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
				+ "}\n" //
				+ "\n" //
				+ "class X implements J {\n" //
				+ "    @Override\n" //
				+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
				+ "    @Override\n" //
				+ "    public int hashCode() { return 0; }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu}, new String[] {expected}, null);
	}

	@Test
	public void testAddOverride1d6NoInterfaceMethods() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "interface I {\n" //
				+ "    void m();\n" //
				+ "    boolean equals(Object obj);\n" //
				+ "}\n" //
				+ "\n" //
				+ "interface J extends I {\n" //
				+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
				+ "}\n" //
				+ "\n" //
				+ "class X implements J {\n" //
				+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
				+ "    public int hashCode() { return 0; }\n" //
				+ "}\n";
		ICompilationUnit cu= pack.createCompilationUnit("I.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "interface I {\n" //
				+ "    void m();\n" //
				+ "    boolean equals(Object obj);\n" //
				+ "}\n" //
				+ "\n" //
				+ "interface J extends I {\n" //
				+ "    void m(); // @Override error in 1.5, not in 1.6\n" //
				+ "}\n" //
				+ "\n" //
				+ "class X implements J {\n" //
				+ "    public void m() {} // @Override error in 1.5, not in 1.6\n" //
				+ "    @Override\n" //
				+ "    public int hashCode() { return 0; }\n" //
				+ "}\n";

		assertNotEquals("The class must be changed", expected, given);
		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu}, new String[] {expected}, null);
	}

	@Test
	public void testConstantsForSystemProperty() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= "" //
				+ "package test1;\n" //
				+ "public class E {\n" //
				+ "    public void simpleCase() {\n" //
				+ "        // Keep this comment\n" //
				+ "        String fs = System.getProperty(\"file.separator\"); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+fs);//$NON-NLS-1$\n" //
				+ "        String ps = System.getProperty(\"path.separator\"); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+ps);//$NON-NLS-1$\n" //
				+ "        String cdn = System.getProperty(\"file.encoding\"); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+cdn);//$NON-NLS-1$\n" //
				+ "        String lsp = System.getProperty(\"line.separator\"); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+lsp);//$NON-NLS-1$\n" //
				+ "        Boolean value = Boolean.parseBoolean(System.getProperty(\"arbitrarykey\")); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+value);//$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.io.File;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void simpleCase() {\n" //
				+ "        // Keep this comment\n" //
				+ "        String fs = File.separator;\n" //
				+ "        System.out.println(\"out:\"+fs);//$NON-NLS-1$\n" //
				+ "        String ps = File.pathSeparator;\n" //
				+ "        System.out.println(\"out:\"+ps);//$NON-NLS-1$\n" //
				+ "        String cdn = System.getProperty(\"file.encoding\"); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+cdn);//$NON-NLS-1$\n" //
				+ "        String lsp = System.getProperty(\"line.separator\"); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+lsp);//$NON-NLS-1$\n" //
				+ "        Boolean value = Boolean.getBoolean(\"arbitrarykey\"); //$NON-NLS-1$\n" //
				+ "        System.out.println(\"out:\"+value);//$NON-NLS-1$\n" //
				+ "    }\n" //
				+ "}\n";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_PATH_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_LINE_SEPARATOR);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_FILE_ENCODING);
		enable(CleanUpConstants.CONSTANTS_FOR_SYSTEM_PROPERTY_BOOLEAN);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.ConstantsCleanUp_description)));
	}

}
