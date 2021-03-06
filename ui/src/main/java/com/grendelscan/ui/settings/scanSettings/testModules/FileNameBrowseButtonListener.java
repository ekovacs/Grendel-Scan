package com.grendelscan.ui.settings.scanSettings.testModules;

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

import com.grendelscan.testing.modules.settings.FileNameOption;
import com.grendelscan.ui.customControls.basic.GText;

public class FileNameBrowseButtonListener extends SelectionAdapter
{
    protected FileNameOption internalOption;
    protected GText internalFilenameTextBox;

    public FileNameBrowseButtonListener(final FileNameOption internalOption, final GText internalFilenameTextBox)
    {
        this.internalOption = internalOption;
        this.internalFilenameTextBox = internalFilenameTextBox;
    }

    @Override
    public void widgetSelected(final SelectionEvent event)
    {
        String filename = null;
        if (internalOption.isDirectory())
        {
            DirectoryDialog dd = new DirectoryDialog(event.display.getActiveShell());
            try
            {
                dd.setFilterPath(new File(".").getCanonicalPath());
            }
            catch (IOException e1)
            {
            }
            dd.setText(internalOption.getName());
            filename = dd.open();
        }
        else
        {
            FileDialog fd = new FileDialog(event.display.getActiveShell(), SWT.SAVE);
            fd.setText(internalOption.getName());
            filename = fd.open();
        }
        if (filename != null)
        {
            internalFilenameTextBox.setText(filename);
            internalOption.setValue(filename);
        }
    }
}
