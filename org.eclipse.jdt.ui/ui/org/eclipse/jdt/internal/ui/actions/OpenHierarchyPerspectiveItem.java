/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.actions;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jface.action.ContributionItem;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.widgets.Menu;import org.eclipse.swt.widgets.MenuItem;import org.eclipse.swt.widgets.Shell;import org.eclipse.ui.IPerspectiveDescriptor;import org.eclipse.ui.IPerspectiveRegistry;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.PartInitException;import org.eclipse.ui.PlatformUI;import org.eclipse.ui.WorkbenchException;import org.eclipse.ui.IWorkbenchPreferenceConstants;import org.eclipse.ui.internal.WorkbenchPlugin;
/** * A menu item for opening the type hierarchy perspective.   * <p> * We don't use an Action so that we can get the selection event and * have access to the modifier key settings. */public class OpenHierarchyPerspectiveItem extends ContributionItem {	private IType[] fTypes;	private IWorkbenchWindow fWindow;	private static IPerspectiveRegistry fgRegistry;	private static String fgMessage= "Select the element to be used as input";	public OpenHierarchyPerspectiveItem(IWorkbenchWindow window, IType[] types) {		fTypes= types;		fWindow= window;		if (fgRegistry == null)			fgRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();	}

	/* (non-Javadoc)	 * Fills the menu with perspective items.	 */	public void fill(Menu menu, int index) {		MenuItem mi = new MenuItem(menu, SWT.PUSH, index);		mi.setText("Open Hierarchy Perspective");		mi.addSelectionListener(new SelectionAdapter() {			public void widgetSelected(SelectionEvent e) {				run(e);			}		});	}
	/**
	 * 
	 */
	public void run(SelectionEvent event) {		IPreferenceStore store= WorkbenchPlugin.getDefault().getPreferenceStore();		String perspectiveSetting=			store.getString(IWorkbenchPreferenceConstants.OPEN_NEW_PERSPECTIVE);		if ((event.stateMask & SWT.ALT) > 0)			perspectiveSetting =				store.getString(IWorkbenchPreferenceConstants.ALTERNATE_OPEN_NEW_PERSPECTIVE);		else {			if ((event.stateMask & SWT.SHIFT) > 0)				perspectiveSetting =					store.getString(IWorkbenchPreferenceConstants.SHIFT_OPEN_NEW_PERSPECTIVE);		}		runWithPerspectiveSetting(perspectiveSetting);	}					private void runWithPerspectiveSetting(String setting) {		IPerspectiveDescriptor pd= fgRegistry.findPerspectiveWithId(JavaUI.ID_HIERARCHYPERSPECTIVE);		if (pd == null) {			JavaPlugin.getDefault().logErrorMessage("Type Hierarchy perspective not found");			return;		}		IType input= null;		if (fTypes.length > 1) {			input= selectType(fTypes, fWindow.getShell(), "Select Type", fgMessage);		} else {			input= fTypes[0];		}		if (input == null)			return;					try {			if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_WINDOW))				fWindow.getWorkbench().openWorkbenchWindow(pd.getId(), input);						else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_PAGE))				fWindow.openPage(pd.getId(), input);						else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_REPLACE)) {				IWorkbenchPage page= fWindow.getActivePage();				if (page != null) {					page.setPerspective(pd);				}			}			EditorUtility.openInEditor(input);		} catch (WorkbenchException e) {			MessageDialog.openError(fWindow.getShell(),				"Problems Opening Perspective",				e.getMessage());		} catch (JavaModelException e) {			MessageDialog.openError(fWindow.getShell(),				"Problems Opening Editor",				e.getMessage());		}	}		/**	 * Shows a dialog to selecting an ambigous source reference.	 * Utility method that can be called by subclassers.	 */	protected IType selectType(IType[] types, Shell shell, String title, String message) {				int flags= (JavaElementLabelProvider.SHOW_DEFAULT);								ElementListSelectionDialog d= new ElementListSelectionDialog(shell, title, null, new JavaElementLabelProvider(flags), true, false);		d.setMessage(message);		if (d.open(types, null) == d.OK) {			Object[] elements= d.getResult();			if (elements != null && elements.length == 1) {				return ((IType)elements[0]);			}		}		return null;	}		/**	 * Return the current perspective setting.	 * REVISIT: need some API to get at this setting.	 * @return String	 */	private String openPerspectiveSetting() {		return WorkbenchPlugin.getDefault().getPreferenceStore().getString(			IWorkbenchPreferenceConstants.OPEN_NEW_PERSPECTIVE);	}
}
