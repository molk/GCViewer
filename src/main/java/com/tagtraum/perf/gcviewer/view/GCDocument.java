package com.tagtraum.perf.gcviewer.view;

import com.tagtraum.perf.gcviewer.model.GCResource;
import com.tagtraum.perf.gcviewer.view.model.GCPreferences;
import com.tagtraum.perf.gcviewer.view.model.GCResourceGroup;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.stream;

/**
 * GCDocument.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class GCDocument extends JInternalFrame {

    private static final Logger LOGGER = Logger.getLogger(GCDocument.class.getName()); 
    
    private final List<ChartPanelView> chartPanelViews = new ArrayList<>();
    private ModelChart modelChartListFacade;
    private boolean showModelMetricsPanel = true;
    private boolean watched;
    private GCPreferences preferences;

    public GCDocument(final GCPreferences preferences, String title) {
        super(title, true, true, true, false);
        
        // keep a copy of the preferences
        this.preferences = new GCPreferences().load();
        this.preferences.setTo(preferences);
        
        showModelMetricsPanel = preferences.isShowModelMetricsPanel();
        modelChartListFacade = new MultiModelChartFacade();
        addComponentListener(new ResizeListener());
        GridBagLayout layout = new GridBagLayout();
        getContentPane().setLayout(layout);
    }
    
    public boolean isShowModelMetricsPanel() {
        return showModelMetricsPanel;
    }

    public void setShowModelMetricsPanel(boolean showModelMetricsPanel) {
        preferences.setBooleanProperty(GCPreferences.SHOW_MODEL_METRICS_PANEL, showModelMetricsPanel);
        boolean mustRelayout = this.showModelMetricsPanel != showModelMetricsPanel;
        this.showModelMetricsPanel = showModelMetricsPanel;
        if (mustRelayout) {
            relayout();
        }
    }

    /**
     * Returns a list of the GCResources displayed in this document.
     * 
     * @return list of GCResources displayed in thsi document
     */
    public List<GCResource> getGCResources() {
        List<GCResource> gcResourceList = new ArrayList<>();
        for (ChartPanelView view : chartPanelViews) {
            gcResourceList.add(view.getGCResource());
        }
        
        return gcResourceList;
    }
    
    public ModelChart getModelChart() {
        return modelChartListFacade;
    }

    public GCPreferences getPreferences() {
        return preferences;
    }
    
    public void addChartPanelView(ChartPanelView chartPanelView) {
        chartPanelView.addPropertyChangeListener(event -> {
			if (ChartPanelView.EVENT_MINIMIZED.equals(event.getPropertyName())) {
				relayout();
			}
			else if (ChartPanelView.EVENT_CLOSED.equals(event.getPropertyName())) {
				removeChartPanelView((ChartPanelView) event.getSource());
			}
		});

        chartPanelViews.add(chartPanelView);
        
        // make sure all models in one document have the same display properties
        if (chartPanelViews.size() > 1) {
            modelChartListFacade.setScaleFactor(modelChartListFacade.getScaleFactor());
            modelChartListFacade.setShowFullGCLines(modelChartListFacade.isShowFullGCLines());
            modelChartListFacade.setShowGCTimesLine(modelChartListFacade.isShowGCTimesLine());
            modelChartListFacade.setShowGCTimesRectangles(modelChartListFacade.isShowGCTimesRectangles());
            modelChartListFacade.setShowIncGCLines(modelChartListFacade.isShowIncGCLines());
            modelChartListFacade.setShowTotalMemoryLine(modelChartListFacade.isShowTotalMemoryLine());
            modelChartListFacade.setShowUsedMemoryLine(modelChartListFacade.isShowUsedMemoryLine());
            modelChartListFacade.setShowDateStamp(modelChartListFacade.isShowDateStamp());
        }
        
        relayout();
    }
    
    /**
     * @return number of chartPanelViews running after remove.
     */
    private int removeChartPanelView(ChartPanelView chartPanelView) {
        chartPanelViews.remove(chartPanelView);
        
        final int nChartPanelViews = chartPanelViews.size();
        if (nChartPanelViews > 0) {        
        	relayout();
        	repaint();
        } 
        else {
            // well, actually the ViewBar of ChartPanelView is not shown, when only one
            // ChartPanelView is left, so this code can never be reached... so, just precaution then?
        	dispose();
        }
        return nChartPanelViews;
    }

    /**
     * Relayouts all chartPanelViews contained in this document. Should always be called, when
     * a change with the chartPanelViews happened (add / remove / minimize / maximize 
     * chartPanelView).
     */
    public void relayout() {
        getContentPane().removeAll();
        String newTitle = "";
        if (chartPanelViews.size() > 0) {
            GCResourceGroup group = new GCResourceGroup(getGCResources());
            newTitle = group.getGroupStringShort();
        }
        setTitle(newTitle);
        
        int row = 0;
        boolean noMaximizedChartPanelView = true;
        ChartPanelView lastMaximizedChartPanelView = getLastMaximizedChartPanelView();
        MasterViewPortChangeListener masterViewPortChangeListener = new MasterViewPortChangeListener();

        for (int i = 0; i < chartPanelViews.size(); i++) {
            final ChartPanelView chartPanelView = chartPanelViews.get(i);
            final ModelChartImpl modelChart = (ModelChartImpl) chartPanelView.getModelChart();
            final ModelMetricsPanel modelMetricsPanel = chartPanelView.getModelMetricsPanel();
            final JTabbedPane modelChartAndDetails = chartPanelView.getModelChartAndDetails();
            modelChart.resetPolygonCache();
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTH;

            constraints.gridy = row;
            if (chartPanelViews.size() > 1 || (chartPanelView.isMinimized() && chartPanelViews.size() == 1)) {
                constraints.gridwidth = 2;
                constraints.weightx = 2;
                //constraints.weighty = 1;
                getContentPane().add(chartPanelView.getViewBar(), constraints);
                row++;
            }
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridy = row;
            constraints.gridwidth = 1;
            constraints.gridheight = 1; //2
            constraints.gridx = 0;
            constraints.weightx = 2;
            constraints.weighty = 2;
            modelChart.setPreferredSize(new Dimension(800, 600));
            modelChartAndDetails.setVisible(!chartPanelView.isMinimized());
            getContentPane().add(modelChartAndDetails, constraints);

            constraints.gridy = row;
            constraints.gridheight = 1;
            constraints.gridx = 1;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.SOUTH;
            getContentPane().add(modelMetricsPanel, constraints);
            modelMetricsPanel.setVisible(showModelMetricsPanel && (!chartPanelView.isMinimized()));

            if (!chartPanelView.isMinimized()) {
                noMaximizedChartPanelView = false;
                final boolean isLastMaximizedChartPanelView = lastMaximizedChartPanelView == chartPanelView;
                // lock viewports with each other...
                // remove old master listeners
                final JViewport viewport = modelChart.getViewport();
                lockChartsToOneScrollbar(viewport, isLastMaximizedChartPanelView, modelChart, masterViewPortChangeListener);

                final DefaultBoundedRangeModel scrollBarModel = (DefaultBoundedRangeModel) modelChart.getHorizontalScrollBar().getModel();

                // clean old change listeners
                stream(scrollBarModel.getChangeListeners())
					.filter(ScrollBarMaximumChangeListener.class::isInstance)
					.forEach(scrollBarModel::removeChangeListener);

                if (isLastMaximizedChartPanelView && isWatched()) {
                    scrollBarModel.addChangeListener(new ScrollBarMaximumChangeListener());
                }
                if (isLastMaximizedChartPanelView) {
                    modelChart.getHorizontalScrollBar().setEnabled(!isWatched());
                }
            }
            row++;
        }
        
        if (noMaximizedChartPanelView) {
            // add dummy panel
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridy = row;
            constraints.weightx = 3;
            constraints.weighty = 3;
            getContentPane().add(new JPanel(), constraints);
        }
        scaleModelChart();
        invalidate();
    }

    private void lockChartsToOneScrollbar(final JViewport viewport, final boolean lastMaximizedChartPanelView, final ModelChartImpl modelChart, MasterViewPortChangeListener masterViewPortChangeListener) {
        // TODO SWINGWORKER: now horizontal scrollbar is shown, when last file has not longest runningtime

		// clean old change listeners
		stream(viewport.getChangeListeners())
			.filter(MasterViewPortChangeListener.class::isInstance)
			.forEach(viewport::removeChangeListener);

        if (lastMaximizedChartPanelView) {
            modelChart.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            // add scrollbar listener
            viewport.addChangeListener(masterViewPortChangeListener);
        } 
        else {
            modelChart.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            masterViewPortChangeListener.addSlaveViewport(viewport);
        }
    }

    public int getChartPanelViewCount() {
        return chartPanelViews.size();
    }

    public ChartPanelView getChartPanelView(int i) {
        return chartPanelViews.get(i);
    }
    
    /**
     * Returns the ChartPanelView that displays <code>gcResource</code>. If none is found, the
     * return value is <code>null</code>.
     * 
     * @param gcResource the resource the ChartPanelView displaying it is looked for
     * @return ChartPanelView-instance or <code>null</code> if no instance was found
     */
    public ChartPanelView getChartPanelView(GCResource gcResource) {
        for (ChartPanelView view : chartPanelViews) {
            if (view.getGCResource().equals(gcResource)) {
                return view;
            }
        }
        
        return null;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
        final JScrollBar horizontalScrollBar = ((ModelChartImpl) getLastMaximizedChartPanelView().getModelChart()).getHorizontalScrollBar();
        if (watched) {
            horizontalScrollBar.setValue(horizontalScrollBar.getMaximum());
        }
        horizontalScrollBar.setEnabled(!watched);
    }

    public boolean isWatched() {
        return watched;
    }

    private static class MasterViewPortChangeListener implements ChangeListener {
        private List<JViewport> slaveViewPorts = new ArrayList<>();

        public void addSlaveViewport(JViewport viewPort) {
            slaveViewPorts.add(viewPort);
        }

        public void stateChanged(ChangeEvent e) {
            JViewport master = (JViewport) e.getSource();
            final int x = master.getViewPosition().x;
            for (JViewport slave : slaveViewPorts) {
                slave.setViewPosition(new Point(x, slave.getViewPosition().y));
            }
        }
    }

    private ChartPanelView getLastMaximizedChartPanelView() {
    	return chartPanelViews.stream()
			.filter(view -> !view.isMinimized())
			.reduce((a, b) -> b).orElse(null);
    }

    private void scaleModelChart() {
        for (ChartPanelView aChartPanelView : chartPanelViews) {
            aChartPanelView.getModelChart().setFootprint(getMaxFootprint());
            aChartPanelView.getModelChart().setMaxPause(getMaxMaxPause());
            aChartPanelView.getModelChart().setRunningTime(getMaxRunningTime());
        }
    }

    private double getMaxRunningTime() {
		return chartPanelViews.stream()
			.mapToDouble(v -> v.getGCResource().getModel().getRunningTime())
			.max().orElse(0.0);
    }

    private long getMaxFootprint() {
		return chartPanelViews.stream()
			.mapToLong(v -> v.getGCResource().getModel().getFootprint())
			.max().orElse(0);
    }

    private double getMaxMaxPause() {
 		return chartPanelViews.stream()
			.mapToDouble(v -> v.getGCResource().getModel().getPause().getMax())
			.max().orElse(0.0);
    }

    private class MultiModelChartFacade implements ModelChart {

        @Override
        public boolean isAntiAlias() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isAntiAlias();
        }

        @Override
        public void setAntiAlias(boolean antiAlias) {
            preferences.setBooleanProperty(GCPreferences.ANTI_ALIAS, antiAlias);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setAntiAlias(antiAlias);
            }
        }

        @Override
        public long getFootprint() {
            if (chartPanelViews.isEmpty()) return 0;
            return chartPanelViews.get(0).getModelChart().getFootprint();
        }

        @Override
        public double getMaxPause() {
            if (chartPanelViews.isEmpty()) return 0;
            return chartPanelViews.get(0).getModelChart().getMaxPause();
        }

        @Override
        public void setRunningTime(double runningTime) {
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setRunningTime(runningTime);
            }
        }

        @Override
        public void setFootprint(long footPrint) {
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setFootprint(footPrint);
            }
        }

        @Override
        public void setMaxPause(double maxPause) {
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setMaxPause(maxPause);
            }
        }

        @Override
        public void setScaleFactor(double scaleFactor) {
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setScaleFactor(scaleFactor);
            }
        }

        @Override
        public double getScaleFactor() {
            if (chartPanelViews.isEmpty()) return 1;
            return chartPanelViews.get(0).getModelChart().getScaleFactor();
        }

        @Override
        public boolean isShowGCTimesLine() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowGCTimesLine();
        }

        @Override
        public void setShowGCTimesLine(boolean showGCTimesLine) {
            preferences.setGcLineProperty(GCPreferences.GC_TIMES_LINE, showGCTimesLine);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowGCTimesLine(showGCTimesLine);
            }
        }

        @Override
        public boolean isShowGCTimesRectangles() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowGCTimesRectangles();
        }

        @Override
        public void setShowGCTimesRectangles(boolean showGCTimesRectangles) {
            preferences.setGcLineProperty(GCPreferences.GC_TIMES_RECTANGLES, showGCTimesRectangles);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowGCTimesRectangles(showGCTimesRectangles);
            }
        }

        @Override
        public boolean isShowFullGCLines() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowFullGCLines();
        }

        @Override
        public void setShowFullGCLines(boolean showFullGCLines) {
            preferences.setGcLineProperty(GCPreferences.FULL_GC_LINES, showFullGCLines);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowFullGCLines(showFullGCLines);
            }
        }

        @Override
        public boolean isShowIncGCLines() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowIncGCLines();
        }

        @Override
        public void setShowIncGCLines(boolean showIncGCLines) {
            preferences.setGcLineProperty(GCPreferences.INC_GC_LINES, showIncGCLines);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowIncGCLines(showIncGCLines);
            }
        }

        @Override
        public boolean isShowTotalMemoryLine() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowTotalMemoryLine();
        }

        @Override
        public void setShowTotalMemoryLine(boolean showTotalMemoryLine) {
            preferences.setGcLineProperty(GCPreferences.TOTAL_MEMORY, showTotalMemoryLine);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowTotalMemoryLine(showTotalMemoryLine);
            }
        }

        @Override
        public void setShowTenured(boolean showTenured) {
            preferences.setGcLineProperty(GCPreferences.TENURED_MEMORY, showTenured);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowTenured(showTenured);
            }
        }

        @Override
        public boolean isShowTenured() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowTenured();
        }

        @Override
        public void setShowYoung(boolean showYoung) {
            preferences.setGcLineProperty(GCPreferences.YOUNG_MEMORY, showYoung);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowYoung(showYoung);
            }
        }

        @Override
        public boolean isShowYoung() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowYoung();
        }

        @Override
        public boolean isShowUsedMemoryLine() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowUsedMemoryLine();
        }

        @Override
        public void setShowUsedMemoryLine(boolean showUsedMemoryLine) {
            preferences.setGcLineProperty(GCPreferences.USED_MEMORY, showUsedMemoryLine);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowUsedMemoryLine(showUsedMemoryLine);
            }
        }

        @Override
        public boolean isShowUsedTenuredMemoryLine() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowUsedTenuredMemoryLine();
        }

        @Override
        public void setShowUsedTenuredMemoryLine(boolean showUsedTenuredMemoryLine) {
            preferences.setGcLineProperty(GCPreferences.USED_TENURED_MEMORY, showUsedTenuredMemoryLine);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowUsedTenuredMemoryLine(showUsedTenuredMemoryLine);
            }
        }

        @Override
        public boolean isShowUsedYoungMemoryLine() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowUsedYoungMemoryLine();
        }

        @Override
        public void setShowUsedYoungMemoryLine(boolean showUsedYoungMemoryLine) {
            preferences.setGcLineProperty(GCPreferences.USED_YOUNG_MEMORY, showUsedYoungMemoryLine);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowUsedYoungMemoryLine(showUsedYoungMemoryLine);
            }
        }

        @Override
        public void setShowInitialMarkLevel(boolean showInitialMarkLevel) {
            preferences.setGcLineProperty(GCPreferences.INITIAL_MARK_LEVEL, showInitialMarkLevel);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowInitialMarkLevel(showInitialMarkLevel);
            }
        }

        @Override
        public boolean isShowInitialMarkLevel() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowInitialMarkLevel();
        }

        @Override
        public void setShowConcurrentCollectionBeginEnd(boolean showConcurrentCollectionBeginEnd) {
            preferences.setGcLineProperty(GCPreferences.CONCURRENT_COLLECTION_BEGIN_END, showConcurrentCollectionBeginEnd);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowConcurrentCollectionBeginEnd(showConcurrentCollectionBeginEnd);
            }
        }

        @Override
        public boolean isShowConcurrentCollectionBeginEnd() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowConcurrentCollectionBeginEnd();
        }

        @Override
        public void resetPolygonCache() {
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().resetPolygonCache();
            }
        }

        @Override
        public void setShowDateStamp(boolean showDateStamp) {
            LOGGER.fine("" + showDateStamp);
            preferences.setGcLineProperty(GCPreferences.SHOW_DATE_STAMP, showDateStamp);
            for (ChartPanelView chartPanelView : chartPanelViews) {
                chartPanelView.getModelChart().setShowDateStamp(showDateStamp);
            }
        }

        @Override
        public boolean isShowDateStamp() {
            if (chartPanelViews.isEmpty()) return false;
            return chartPanelViews.get(0).getModelChart().isShowDateStamp();

        }
    }

    private class ScrollBarMaximumChangeListener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            if (isWatched()) {
                BoundedRangeModel model = (BoundedRangeModel) e.getSource();
                model.setValue(model.getMaximum());
            }
        }

    }

    /**
     * @author <a href="mailto:gcviewer@gmx.ch">Joerg Wuethrich</a>
     */
    private class ResizeListener extends ComponentAdapter {

        @Override
        public void componentResized(ComponentEvent e) {
            modelChartListFacade.resetPolygonCache();
        }

    }

}
