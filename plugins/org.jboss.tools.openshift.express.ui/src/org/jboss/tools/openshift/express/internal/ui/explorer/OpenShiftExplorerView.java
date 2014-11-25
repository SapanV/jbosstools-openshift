/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.explorer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.part.PageBook;
import org.jboss.tools.openshift.core.Connection;
import org.jboss.tools.openshift.core.ConnectionType;
import org.jboss.tools.openshift.express.core.IConnectionsModelListener;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectionWizard;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;

/**
 * @author Xavier Coulon
 */
public class OpenShiftExplorerView extends CommonNavigator implements IConnectionsModelListener {

	private Control connectionsPane;
	private Control explanationsPane;
	private PageBook pageBook;

	@Override
	protected Object getInitialInput() {
		return ConnectionsModelSingleton.getInstance();
	}

	@Override
	protected CommonViewer createCommonViewer(Composite parent) {
		CommonViewer viewer = super.createCommonViewer(parent);
		new OpenShiftExplorerContextsHandler(viewer);
		ConnectionsModelSingleton.getInstance().addListener(this);
		return viewer;
	}

	@Override
	public void dispose() {
		ConnectionsModelSingleton.getInstance().removeListener(this);
		super.dispose();
	}

	public void refreshViewer(final Connection connection) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				CommonViewer viewer = getCommonViewer();
				if (DisposeUtils.isDisposed(viewer)) {
					return;
				}
				if (connection != null) {
					viewer.refresh(connection);
				} else {
					viewer.refresh();
				}
				showConnectionsOrExplanations(connectionsPane, explanationsPane);
			}
		});
	}

	@Override
	public void connectionAdded(Connection connection, ConnectionType type) {
		refreshViewer(null);
	}

	@Override
	public void connectionRemoved(Connection connection, ConnectionType type) {
		refreshViewer(null);
	}

	@Override
	public void connectionChanged(Connection connection, ConnectionType type) {
		refreshViewer(connection);
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		this.pageBook = new PageBook(parent, SWT.NONE);

		super.createPartControl(pageBook);

		this.connectionsPane = getCommonViewer().getControl();
		this.explanationsPane = createExplanationPane(connectionsPane, pageBook, toolkit);
		showConnectionsOrExplanations(connectionsPane, explanationsPane);
	}

	private Control createExplanationPane(Control connectionsPane, PageBook pageBook, FormToolkit kit) {
		Form form = kit.createForm(pageBook);
		Composite composite = form.getBody();
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);

		Link link = new Link(composite, SWT.NONE);
		link.setText(OpenshiftUIMessages.NoConnectionsAreAvailable);
		link.setBackground(pageBook.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL).grab(true, false).applyTo(link);
		link.addSelectionListener(onExplanationClicked(connectionsPane, link));
		return form;
	}

	private SelectionAdapter onExplanationClicked(final Control connectionsPane, final Control explanationPane) {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ConnectionWizard wizard = new ConnectionWizard();
				WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getModalDialogShellProvider()
						.getShell(), wizard);
				if (dialog.open() == Window.OK) {
					showConnectionsOrExplanations(connectionsPane, explanationPane);
				}
			}
		};
	}

	private void showConnectionsOrExplanations(Control connectionsPane, Control explanationsPane) {
		if (ConnectionsModelSingleton.getInstance().getAllConnections().length < 1) {
			pageBook.showPage(explanationsPane);
		} else {
			pageBook.showPage(connectionsPane);
		}
	}
	
	private static class OpenShiftExplorerContextsHandler extends Contexts {

		private static final String CONNECTION_CONTEXT = "org.jboss.tools.openshift.explorer.context.connection";
		private static final String APPLICATION_CONTEXT = "org.jboss.tools.openshift.explorer.context.application";
		private static final String DOMAIN_CONTEXT = "org.jboss.tools.openshift.explorer.context.domain";
		
		OpenShiftExplorerContextsHandler(CommonViewer viewer) {
			viewer.getControl().addFocusListener(onFocusLost());
			viewer.addSelectionChangedListener(onSelectionChanged());
		}
		
		private FocusAdapter onFocusLost() {
			return new FocusAdapter() {
				
				@Override
				public void focusLost(FocusEvent event) {
					deactivateCurrent();
				}
			};
		}

		private ISelectionChangedListener onSelectionChanged() {
			return new ISelectionChangedListener() {

				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					ISelection selection = event.getSelection();
					if (UIUtils.isFirstElementOfType(IDomain.class, selection)) {
						activate(DOMAIN_CONTEXT);
					} else if (UIUtils.isFirstElementOfType(IApplication.class, selection)) {
						activate(APPLICATION_CONTEXT);
					} else if (UIUtils.isFirstElementOfType(org.jboss.tools.openshift.express.internal.core.connection.Connection.class, selection)) {
						// must be checked after domain, application, adapter may convert
						// any resource to a connection
						activate(CONNECTION_CONTEXT);
					}
				}
			};
		}
	}
	
	private static class Contexts {
		
		private IContextActivation contextActivation;
		
		public void activate(String contextId) {
			deactivateCurrent();
			IContextService service = getService();
			this.contextActivation = service.activateContext(contextId);
		}
		
		public void deactivateCurrent() {
			if (contextActivation != null) {
				IContextService service = getService();
				service.deactivateContext(contextActivation);
			}
		}
		
		private IContextService getService() {
			return (IContextService) PlatformUI.getWorkbench().getService(IContextService.class);
		}
	}

}
