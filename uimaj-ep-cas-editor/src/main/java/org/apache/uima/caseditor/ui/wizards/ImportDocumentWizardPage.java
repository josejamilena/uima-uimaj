/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.caseditor.ui.wizards;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.CorpusElement;
import org.apache.uima.caseditor.editor.DocumentFormat;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * The main page of the <code>ImportDocumentWizard</code>.
 */
final class ImportDocumentWizardPage extends WizardPage {

  private IPath importDestinationPath;

  private String importEncoding;
  
  private DocumentFormat documentFormat;
  
  private TableViewer fileTable;

  private CorpusElement corpusElement;

  protected ImportDocumentWizardPage(String pageName,
      IStructuredSelection currentResourceSelection) {
    super(pageName);

    setMessage("Please select the documents to import.");

    if (!currentResourceSelection.isEmpty()) {
      if (currentResourceSelection.getFirstElement() instanceof CorpusElement) {
        corpusElement = (CorpusElement) currentResourceSelection.getFirstElement();
        importDestinationPath = corpusElement.getResource().getFullPath();
      }
    }

    setPageComplete(false);
  }

  private void computePageComplete() {
	  
	boolean isEncodingSupported = false;
	
	try {
		isEncodingSupported = Charset.isSupported(importEncoding);
	}
	catch (IllegalCharsetNameException e) {
		// Name of the Charset is incorrect, that means
		// it cannot exist
		
	}
    setPageComplete(importDestinationPath != null && 
    		fileTable.getTable().getItemCount() > 0
    		&& isEncodingSupported);
  }

  public void createControl(Composite parent) {

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));

    fileTable = new TableViewer(composite);
    GridDataFactory.fillDefaults().grab(true, true).span(2, 4).applyTo(fileTable.getControl());

    Button addButton = new Button(composite, SWT.PUSH);
    addButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    addButton.setText("Add");
    addButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Opens a file dialog and adds the selected files to the file table viewer.
       */
      public void widgetSelected(SelectionEvent e) {

        // open a file dialog
        FileDialog fd = new FileDialog(Display.getCurrent().getActiveShell(), SWT.MULTI);
        fd.setText("Choose text files");
        fd.setFilterExtensions(new String[] { "*.txt;*.rtf", "*.*"});
        fd.setFilterNames(new String[] {"Text Files", "All Files (*)"});
        if (fd.open() != null) {
          for (String fileItem : fd.getFileNames()) {
            fileTable.add(new File(fd.getFilterPath() + File.separator + fileItem));
          }

          computePageComplete();
        }
      }
    });

    Button removeButton = new Button(composite, SWT.PUSH);
    removeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    removeButton.setText("Remove");
    removeButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Removes selected elements from the file table viewer.
       */
      @SuppressWarnings("unchecked")
      public void widgetSelected(SelectionEvent e) {
        IStructuredSelection selection = (IStructuredSelection) fileTable.getSelection();

        Iterator seletionIterator = selection.iterator();

        Object selectedElements[] = new Object[selection.size()];

        for (int i = 0; i < selection.size(); i++) {
          selectedElements[i] = seletionIterator.next();
        }

        fileTable.remove(selectedElements);

        computePageComplete();
      }
    });

    Button selectAllButton = new Button(composite, SWT.PUSH);
    selectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    selectAllButton.setText("Select All");
    selectAllButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Selects all elements in the file table viewer.
       */
      public void widgetSelected(SelectionEvent e) {
        fileTable.getTable().selectAll();
        fileTable.setSelection(fileTable.getSelection());
      }
    });

    Button deselectAllButton = new Button(composite, SWT.PUSH);
    deselectAllButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    deselectAllButton.setText("Deselect All");
    deselectAllButton.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Deselects all elements in the file table viewer.
       */
      public void widgetSelected(SelectionEvent e) {
        fileTable.getTable().deselectAll();
        fileTable.setSelection(fileTable.getSelection());
      }
    });

    // Into Corpus folder 
    Label corpusFolderLabel = new Label(composite, SWT.NONE);
    corpusFolderLabel.setText("Into corpus:");

    final Text corpusText = new Text(composite, SWT.READ_ONLY | SWT.BORDER);
    corpusText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    if (importDestinationPath != null) {
      corpusText.setText(importDestinationPath.toString());
    }

    Button browseForCorpusFolder = new Button(composite, SWT.NONE);
    browseForCorpusFolder.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.VERTICAL_ALIGN_BEGINNING));
    browseForCorpusFolder.setText("Browse");
    browseForCorpusFolder.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
        // never called
      }

      /**
       * Opens the corpus folder chooser dialog and shows the chosen dialog in the corpus folder
       * text field.
       */
      public void widgetSelected(SelectionEvent e) {

        final ElementTreeSelectionDialog folderSelectionDialog = new ElementTreeSelectionDialog(
                getShell(), new DecoratingLabelProvider(new WorkbenchLabelProvider(), PlatformUI
                        .getWorkbench().getDecoratorManager().getLabelDecorator()),
                new BaseWorkbenchContentProvider());

        folderSelectionDialog.addFilter(new CorpusElementFilter());

        if (corpusElement != null) {
          folderSelectionDialog.setInitialSelection(corpusElement);
        }

        folderSelectionDialog.setInput(org.apache.uima.caseditor.CasEditorPlugin.getNlpModel());

        folderSelectionDialog.setTitle("Choose corpus");
        folderSelectionDialog.setMessage("Please choose a corpus.");

        folderSelectionDialog.setValidator(new ISelectionStatusValidator() {
          public IStatus validate(Object[] selection) {

            if (selection.length == 1 && selection[0] instanceof CorpusElement) {
              return new Status(IStatus.OK, CasEditorPlugin.ID, 0, "", null);
            }

            return new Status(IStatus.ERROR, CasEditorPlugin.ID, 0, "Please select a corpus!", null);
          }
        });

        folderSelectionDialog.open();

        Object[] results = folderSelectionDialog.getResult();

        if (results != null) {
          // validator makes sure that one CorpusElement is selected
          corpusElement = (CorpusElement) results[0];

          IFolder corpusFolder = (IFolder) corpusElement.getResource();
          importDestinationPath = corpusFolder.getFullPath();

          corpusText.setText(importDestinationPath.toString());

          computePageComplete();
        }
      }
    });

    Group importOptions = new Group(composite, SWT.NONE);
    importOptions.setText("Options");
    GridLayout importOptionsGridLayout = new GridLayout();
    importOptionsGridLayout.numColumns = 2;
    importOptions.setLayout(importOptionsGridLayout);
    GridData importOptionsGridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
    importOptionsGridData.horizontalSpan = 3;
    importOptions.setLayoutData(importOptionsGridData);
	
    // Text file encoding
    Label encodingLabel = new Label(importOptions, SWT.NONE);
    encodingLabel.setText("Text Encoding:");
    
    // combo box ...
    final Combo encodingCombo = new Combo(importOptions, SWT.NONE);
    encodingCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    
    importEncoding = Charset.defaultCharset().displayName();
    
    Set<String> charsets = new HashSet<String>();
    charsets.add("US-ASCII");
    charsets.add("ISO-8859-1");
    charsets.add("UTF-8");
    charsets.add("UTF-16BE");
    charsets.add("UTF-16LE");
    charsets.add("UTF-16");
    charsets.add(importEncoding);
    
    encodingCombo.setItems(charsets.toArray(new String[charsets.size()]));
    encodingCombo.setText(importEncoding);
    encodingCombo.addSelectionListener(new SelectionListener() {
		
		public void widgetSelected(SelectionEvent e) {
			importEncoding = encodingCombo.getText();
			computePageComplete();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	});
    
    encodingCombo.addKeyListener(new KeyListener() {
		
		public void keyReleased(KeyEvent e) {
			importEncoding = encodingCombo.getText();
			computePageComplete();
		}
		
		public void keyPressed(KeyEvent e) {
		}
	});
    
    Label casFormatLabel = new Label(importOptions, SWT.NONE);
    casFormatLabel.setText("Cas Format:");
  
    final Combo casFormatCombo = new Combo(importOptions, SWT.READ_ONLY);
    casFormatCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    casFormatCombo.setItems(new String[]{DocumentFormat.XMI.toString(), DocumentFormat.XCAS.toString()});
    documentFormat = DocumentFormat.XMI;
    casFormatCombo.select(0);
    
    casFormatCombo.addSelectionListener(new SelectionListener() {
		
		public void widgetSelected(SelectionEvent e) {
			documentFormat = DocumentFormat.valueOf(casFormatCombo.getText());
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	});
    
    
    computePageComplete();
    
    setControl(composite);
  }

  /**
   * Retrieves the import destination path.
   *
   * @return the path or null if none
   */
  IPath getImportDestinationPath() {
    return importDestinationPath;
  }

  List<File> getFilesToImport() {

    List<File> files = new ArrayList<File>(fileTable.getTable().getItemCount());

    for (int i = 0; i < fileTable.getTable().getItemCount(); i++) {
      files.add((File) fileTable.getElementAt(i));
    }

    return files;
  }
  
  String getTextEncoding() {
	  return importEncoding;
  }
  
  DocumentFormat getCasFormat() {
	  return documentFormat;
  }
}
