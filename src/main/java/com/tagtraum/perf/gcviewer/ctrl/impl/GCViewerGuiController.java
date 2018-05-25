package com.tagtraum.perf.gcviewer.ctrl.impl;

import com.tagtraum.perf.gcviewer.ctrl.GCModelLoaderController;
import com.tagtraum.perf.gcviewer.ctrl.action.OpenFile;
import com.tagtraum.perf.gcviewer.model.GCResource;
import com.tagtraum.perf.gcviewer.view.ActionCommands;
import com.tagtraum.perf.gcviewer.view.GCDocument;
import com.tagtraum.perf.gcviewer.view.GCViewerGui;
import com.tagtraum.perf.gcviewer.view.model.GCPreferences;
import com.tagtraum.perf.gcviewer.view.model.GCResourceGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map.Entry;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.util.stream.Collectors.toList;
import static javax.swing.SwingUtilities.invokeAndWait;

/**
 * Main controller class of GCViewer. 
 * 
 * @author <a href="mailto:gcviewer@gmx.ch">Joerg Wuethrich</a>
 * <p>created on: 11.02.2014</p>
 */
public class GCViewerGuiController extends WindowAdapter {
    
    void applyPreferences(GCViewerGui gui, GCPreferences preferences) {
        // default visibility to be able to access it from unittests
        gui.setPreferences(preferences);

        if (!preferences.isPropertiesLoaded()) {
            gui.setBounds(0, 0, 800, 600);
		}
		else {
            for (Entry<String, JCheckBoxMenuItem> menuEntry : gui.getMenubar().getViewMenuItems().entrySet()) {
                JCheckBoxMenuItem item = menuEntry.getValue();
                item.setState(preferences.getGcLineProperty(menuEntry.getKey()));
                
                // TODO necessary? state is set above; no GCDocument open at this moment
                //viewMenuActionListener.actionPerformed(new ActionEvent(item, 0, item.getActionCommand()));
            }

            gui.setBounds(boundsFrom(preferences));

            String lastfile = preferences.getLastFile();

            if (lastfile != null) {
                ((OpenFile)gui.getActionMap().get(ActionCommands.OPEN_FILE.toString())).setSelectedFile(new File(lastfile));
            }
            // recent files (add in reverse order, so that the order in recentMenu is correct
            List<String> recentFiles = preferences.getRecentFiles();
            for (int i = recentFiles.size()-1; i >= 0; --i) {
                String filename = recentFiles.get(i);
                if (filename.length() > 0) {
                    gui.getMenubar().getRecentGCResourcesModel().add(filename);
                }
            }
        }
    }

	private static Rectangle boundsFrom(GCPreferences preferences) {
		// TODO: fix for automatic resize when setting the window width 'almost or equal to' the screen width
		final int screenWidth = Double.valueOf(java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth()).intValue();

		return new Rectangle(
			preferences.getWindowX(),
			preferences.getWindowY(),
			Math.min(preferences.getWindowWidth(), screenWidth - 11), // 10 is not enough ...
			preferences.getWindowHeight());
	}

	private void closeAllButSelectedDocument(GCViewerGui gui) {
        if (gui.getSelectedGCDocument() != null) {
            GCDocument selected = gui.getSelectedGCDocument();
            for (int i = gui.getDesktopPane().getComponentCount()-1; i > 0; --i) {
                if (gui.getDesktopPane().getComponent(i) != selected) {
                    ((JInternalFrame)gui.getDesktopPane().getComponent(i)).dispose();
                }
            }

            gui.getSelectedGCDocument().doDefaultCloseAction();
        }
    }
    
    /**
     * Copies values that are stored in menu items into <code>GCPreferences</code> instance.
     * 
     * @param gui source to copy values from
     * @return <code>GCPreferences</code> with current values
     */
    private static GCPreferences copyPreferencesFromGui(GCViewerGui gui) {
        GCPreferences preferences = gui.getPreferences();

        for (Entry<String, JCheckBoxMenuItem> menuEntry : gui.getMenubar().getViewMenuItems().entrySet()) {
            JCheckBoxMenuItem item = menuEntry.getValue();
            preferences.setGcLineProperty(item.getActionCommand(), item.getState());
        }

        preferences.setWindowWidth(gui.getWidth());
        preferences.setWindowHeight(gui.getHeight());
        preferences.setWindowX(gui.getX());
        preferences.setWindowY(gui.getY());

        OpenFile openFileAction = (OpenFile)gui.getActionMap().get(ActionCommands.OPEN_FILE.toString());

        if (openFileAction.getLastSelectedFiles().length != 0) {
            preferences.setLastFile(openFileAction.getLastSelectedFiles()[0].getAbsolutePath());
        }
        
        // recent files
		preferences.setRecentFiles(
			gui.getMenubar().getRecentGCResourcesModel().getResourceNameGroups().stream()
				.map(GCResourceGroup::getUrlGroupString)
				.collect(toList()));

        return preferences;
    }

    /**
     * Start graphical user interface and load a log file (resourceName - if not <code>null</code>).
     * 
     * @param gcResource {@link GCResource} to be loaded at startup or <code>null</code>
     * @throws InvocationTargetException Some problem trying to start the gui
     * @throws InterruptedException Some problem trying to start the gui
     */
    public void startGui(final GCResource gcResource) throws InvocationTargetException, InterruptedException {
		final GCViewerGui gcViewerGui = new GCViewerGui();
        final GCModelLoaderController modelLoaderController = new GCModelLoaderControllerImpl(gcViewerGui);

        invokeAndWait(() -> {
			new GCViewerGuiBuilder().initGCViewerGui(gcViewerGui, modelLoaderController);
			applyPreferences(gcViewerGui, new GCPreferences().load());
			gcViewerGui.addWindowListener(GCViewerGuiController.this);
			setDefaultUncaughtExceptionHandler(new GCViewerUncaughtExceptionHandler(gcViewerGui));
			gcViewerGui.setVisible(true);
		});

        if (gcResource != null) {
			invokeAndWait(() -> modelLoaderController.open(gcResource));
		}
    }

    /**
     * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosing(WindowEvent e) {
        // TODO SWINGWORKER fix closing of main window with correct storing of preferences
        closeAllButSelectedDocument(((GCViewerGui)e.getWindow()));
        
        GCPreferences preferences = copyPreferencesFromGui(((GCViewerGui)e.getWindow()));
        preferences.store();
        e.getWindow().dispose();
    }


}
