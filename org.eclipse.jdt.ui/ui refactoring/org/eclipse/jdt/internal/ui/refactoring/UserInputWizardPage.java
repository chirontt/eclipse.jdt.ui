/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Arrays;import java.util.Iterator;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.IStructuredContentProvider;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.core.resources.IFile;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IFileEditorInput;import org.eclipse.ui.dialogs.ListSelectionDialog;import org.eclipse.jdt.internal.core.refactoring.DebugUtils;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * An abstract wizard page that can be used to implement user input pages for 
 * refactoring wizards. Usually user input pages are pages shown at the beginning 
 * of a wizard. As soon as the "last" user input page is left a corresponding 
 * precondition check is executed.
 */
public abstract class UserInputWizardPage extends RefactoringWizardPage {

	private boolean fIsLastUserPage;

	private static class SaveDialog extends ListSelectionDialog {
		public SaveDialog(Shell parent, Object input,
				IStructuredContentProvider contentProvider,
				ILabelProvider labelProvider,
				String message) {
			super(parent, input, contentProvider, labelProvider, message);
		}
		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			final Button check= new Button(result, SWT.CHECK);
			check.setText(RefactoringMessages.getString("UserInputWizardPage.always_save")); //$NON-NLS-1$
			check.setSelection(RefactoringPreferences.getSaveAllEditors());
			check.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					RefactoringPreferences.setSaveAllEditors(check.getSelection());
				}
			});
			return result;
		}
	}
	
	/**
	 * Creates a new user input page.
	 * @param name the page's name.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public UserInputWizardPage(String name, boolean isLastUserPage) {
		super(name);
		fIsLastUserPage= isLastUserPage;
	}
	
	/* (non-Javadoc)
	 * Method declared in WizardPage
	 */
	public void setVisible(boolean visible) {
		if (visible)
			getRefactoringWizard().setChange(null);
		super.setVisible(visible);
	}
	
	/* (non-JavaDoc)
	 * Method declared in IWizardPage.
	 */
	public IWizardPage getNextPage() {
		if (fIsLastUserPage) {
			if (!saveOpenEditors())
				return this;
			else 
				return getRefactoringWizard().computeUserInputSuccessorPage(this);
		} else {
			return super.getNextPage();
		}
	}
	
	/* (non-JavaDoc)
	 * Method declared in IWizardPage.
	 */
	public boolean canFlipToNextPage() {
		if (fIsLastUserPage) {
			// we can't call getNextPage to determine if flipping is allowed since computing
			// the next page is quite expensive (checking preconditions and creating a
			// change). So we say yes if the page is complete.
			return isPageComplete();
		} else {
			return super.canFlipToNextPage();
		}
	}
	
	/* (non-JavaDoc)
	 * Method defined in RefactoringWizardPage
	 */
	protected boolean performFinish() {
		if (fIsLastUserPage && (!saveOpenEditors()))
			return false;
		
		RefactoringWizard wizard= getRefactoringWizard();
		int threshold= RefactoringPreferences.getCheckPassedSeverity();
		RefactoringStatus activationStatus= wizard.getActivationStatus();
		RefactoringStatus inputStatus= null;
		RefactoringStatus status= new RefactoringStatus();
		Refactoring refactoring= getRefactoring();
		boolean result= false;
		
		if (activationStatus != null && activationStatus.getSeverity() > threshold) {
			inputStatus= wizard.checkInput();
		} else {
			CreateChangeOperation create= new CreateChangeOperation(refactoring, CreateChangeOperation.CHECK_INPUT); 
			create.setCheckPassedSeverity(threshold);
			
			PerformChangeOperation perform= new PerformChangeOperation(create);
			perform.setCheckPassedSeverity(threshold);
			
			result= wizard.performFinish(perform);
			if (!result)
				return false;
			inputStatus= create.getStatus();
		}
		
		status.merge(activationStatus);
		status.merge(inputStatus);
		
		if (status.getSeverity() > threshold) {
			wizard.setStatus(status);
			IWizardPage nextPage= wizard.getPage(ErrorWizardPage.PAGE_NAME);
			wizard.getContainer().showPage(nextPage);
			return false;
		}
		
		return result;	
	}
	
	//---- Save open editors -------------------------------------------------------------

	/**
	 * Creates a runnable to be used inside an operation to save all editors.
	 */
	private static IRunnableWithProgress createRunnable(final List editorsToSave) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				Iterator iter= editorsToSave.iterator();
				while (iter.hasNext())
					((IEditorPart) iter.next()).doSave(monitor);
			}
		};
	}
		
	private static ILabelProvider createLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}
	
	/**
	 * @return null on cancel and a list of selected elements otherwise
	 * returns an empty array if there were no editors to save.
	 */
	private static List getEditorsToSave(List unsavedEditors){
		if (RefactoringPreferences.getSaveAllEditors()) //must save everything
			return unsavedEditors;
		if (unsavedEditors == null || unsavedEditors.isEmpty())
			return new ArrayList(0); //as promised in the contract
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		String message= RefactoringMessages.getString("UserInputWizardPage.warning"); //$NON-NLS-1$
		String title= RefactoringMessages.getString("UserInputWizardPage.select_to_save"); //$NON-NLS-1$
		SaveDialog dialog= new SaveDialog(parent, unsavedEditors, new ListContentProvider(), createLabelProvider(), message);
		dialog.setTitle(title);
		dialog.setBlockOnOpen(true);
		dialog.setInitialSelections(unsavedEditors.toArray());	
		if (dialog.open() == ListSelectionDialog.CANCEL)
			return null;
		else
			return Arrays.asList(dialog.getResult());
	}
	
	//a-b
	private static List createDifference(List a, List b){
		List temp= new ArrayList(a); //make sure you can remove
		if (b == null)
			return temp;
		temp.removeAll(b);
		return temp;
	}
	
	private static List getFiles(List editorParts){
		List result= new ArrayList(editorParts.size());
		for (Iterator iter= editorParts.iterator(); iter.hasNext(); ){
			IEditorPart each= (IEditorPart)iter.next();
			IEditorInput input= each.getEditorInput();
			if (input instanceof IFileEditorInput)
				result.add(((IFileEditorInput)input).getFile());
		}
		return result;
	}
		
	/**
	 * Save open editors to make sure the java search and AST is working correctly.
	 * Returns <code>true</code> if saving was successful. Otherwise <code>false</code> is returned.
	 */
	private boolean saveOpenEditors() {
		List unsavedEditorsList= Arrays.asList(JavaPlugin.getDirtyEditors());
		List editorsToSave= getEditorsToSave(unsavedEditorsList);
		List unsavedFiles= getFiles(createDifference(unsavedEditorsList, editorsToSave));
		//XXX no the nicest place to do this
		getRefactoring().setUnsavedFiles((IFile[])unsavedFiles.toArray(new IFile[unsavedFiles.size()]));
		if (editorsToSave == null) //saving canceled, so unsuccesful
			return false;
		if (editorsToSave.isEmpty()) //nothing to do
			return true;
					
		try {
			// Save isn't cancelable.
			//XXX: 1GCQYJK: ITPUI:WIN2000 - Invalid thread access when saving an editor
			getContainer().run(false, false, createRunnable(editorsToSave));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), RefactoringMessages.getString("UserInputWizardPage.refactoring"), RefactoringMessages.getString("UserInputWizardPage.unexpected_error")); //$NON-NLS-2$ //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {
			// Can't happen. Operation isn't cancelable.
		}
		return true;
	}
}