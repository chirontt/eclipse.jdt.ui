/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ControlStatementsFix;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ControlStatementsCleanUp extends AbstractCleanUp {

	public ControlStatementsCleanUp(Map<String, String> options) {
		super(options);
    }

	public ControlStatementsCleanUp() {
		super();
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	private boolean requireAST() {
		boolean useBlocks= isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);

		if (!useBlocks)
			return false;

		return isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS) ||
		       isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER) ||
		       isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW);
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();
		if (compilationUnit == null)
			return null;

		boolean useBlocks= isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		if (!useBlocks)
			return null;

		return ControlStatementsFix.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS),
				isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER),
				isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW));
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS) && isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS))
			result.add(MultiFixMessages.CodeStyleMultiFix_ConvertSingleStatementInControlBodyToBlock_description);
		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS) && isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER))
			result.add(MultiFixMessages.ControlStatementsCleanUp_RemoveUnnecessaryBlocks_description);
		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS) && isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW))
			result.add(MultiFixMessages.ControlStatementsCleanUp_RemoveUnnecessaryBlocksWithReturnOrThrow_description);

		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder buf= new StringBuilder();

		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS) && isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS)) {
			buf.append("if (obj == null) {\n"); //$NON-NLS-1$
			buf.append("    throw new IllegalArgumentException();\n"); //$NON-NLS-1$
			buf.append("}\n"); //$NON-NLS-1$

			buf.append("if (ids.length > 0) {\n"); //$NON-NLS-1$
			buf.append("    System.out.println(ids[0]);\n"); //$NON-NLS-1$
			buf.append("} else {\n"); //$NON-NLS-1$
			buf.append("    return;\n"); //$NON-NLS-1$
			buf.append("}\n"); //$NON-NLS-1$
		} else if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS) && isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER)){
			buf.append("if (obj == null)\n"); //$NON-NLS-1$
			buf.append("    throw new IllegalArgumentException();\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$

			buf.append("if (ids.length > 0)\n"); //$NON-NLS-1$
			buf.append("    System.out.println(ids[0]);\n"); //$NON-NLS-1$
			buf.append("else\n"); //$NON-NLS-1$
			buf.append("    return;\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
		} else if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS) && isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NO_FOR_RETURN_AND_THROW)) {
			buf.append("if (obj == null)\n"); //$NON-NLS-1$
			buf.append("    throw new IllegalArgumentException();\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$

			buf.append("if (ids.length > 0) {\n"); //$NON-NLS-1$
			buf.append("    System.out.println(ids[0]);\n"); //$NON-NLS-1$
			buf.append("} else \n"); //$NON-NLS-1$
			buf.append("    return;\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
		} else {
			buf.append("if (obj == null) {\n"); //$NON-NLS-1$
			buf.append("    throw new IllegalArgumentException();\n"); //$NON-NLS-1$
			buf.append("}\n"); //$NON-NLS-1$

			buf.append("if (ids.length > 0) {\n"); //$NON-NLS-1$
			buf.append("    System.out.println(ids[0]);\n"); //$NON-NLS-1$
			buf.append("} else \n"); //$NON-NLS-1$
			buf.append("    return;\n"); //$NON-NLS-1$
			buf.append("\n"); //$NON-NLS-1$
		}

		return buf.toString();
	}

}
