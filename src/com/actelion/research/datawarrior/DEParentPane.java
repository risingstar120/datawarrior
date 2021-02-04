/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior;

import com.actelion.research.gui.dock.ShadowBorder;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.RuntimePropertyListener;
import com.actelion.research.table.view.CompoundTableView;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class DEParentPane extends JComponent implements DetailPopupProvider  {
    private static final long serialVersionUID = 0x20060904;

    private DEFrame mParentFrame;
    private DatabaseActions mDatabaseActions;
	private DECompoundTableModel mTableModel;
	private JSplitPane mMainSplitPane;
	private DEMainPane mTabbedMainViews;
	private JSplitPane mRightSplitPane;
	private DEPruningPanel mPruningPanel;
	private DEDetailPane mTabbedDetailViews;
	private ArrayList<RuntimePropertyListener> mRPListener;

	public DEParentPane(DEFrame parent, DECompoundTableModel tableModel, DEDetailPane detailPane, DatabaseActions databaseActions) {
	    mParentFrame = parent;
	    mTableModel = tableModel;
	    mDatabaseActions = databaseActions;

	    setLayout(new BorderLayout());

	    DEStatusPanel statusPanel = new DEStatusPanel(mTableModel, mTabbedMainViews);
		add(statusPanel, BorderLayout.SOUTH);

		mTabbedDetailViews = detailPane;
		mTabbedDetailViews.setBorder(new ShadowBorder(1,1,3,6));
		mTabbedMainViews = new DEMainPane(mParentFrame, mTableModel, mTabbedDetailViews, statusPanel, this);

		mPruningPanel = new DEPruningPanel(mParentFrame, this, mTableModel);
		mPruningPanel.setBorder(new ShadowBorder(4,1,3,6));

		mMainSplitPane = new JSplitPane();
	    mMainSplitPane.setBorder(null);
		mMainSplitPane.setOneTouchExpandable(true);
	    mMainSplitPane.setContinuousLayout(true);
	    mMainSplitPane.setResizeWeight(0.75);
//		mMainSplitPane.setDividerSize((int)(HiDPIHelper.getUIScaleFactor()*10f));

		mRightSplitPane = new JSplitPane();
		mRightSplitPane.setBorder(null);
	    mRightSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
	    mRightSplitPane.setMinimumSize(new Dimension(100, 200));
	    mRightSplitPane.setPreferredSize(new Dimension(100, 200));
		mRightSplitPane.setOneTouchExpandable(true);
	    mRightSplitPane.setContinuousLayout(true);
	    mRightSplitPane.setResizeWeight(0.7);
//		mRightSplitPane.setDividerSize((int)(HiDPIHelper.getUIScaleFactor()*10f));

	    add(mMainSplitPane, BorderLayout.CENTER);
	    mMainSplitPane.add(mTabbedMainViews, JSplitPane.LEFT);
	    mMainSplitPane.add(mRightSplitPane, JSplitPane.RIGHT);
	    mRightSplitPane.add(mPruningPanel, JSplitPane.TOP);
	    mRightSplitPane.add(mTabbedDetailViews, JSplitPane.BOTTOM);

		mRPListener = new ArrayList<>();
		}

	@Override
	public void updateUI() {
		super.updateUI();
		SwingUtilities.invokeLater(() -> {
            mRightSplitPane.setBorder(null);
			if (mTabbedMainViews.getComponentCount() != 0)
	            recursivelyRemoveSplitPaneBorders(mTabbedMainViews.getComponent(0));
			} );
		}

	/**
	 * The apple aqua look&feel uses a border that draws an ugly line around any split pane
	 */
	private void recursivelyRemoveSplitPaneBorders(Component c) {
		if (c != null && c instanceof JTabbedPane) {
			((JTabbedPane)c).setBorder(null);
			}
		if (c != null && c instanceof JSplitPane) {
			JSplitPane sp = (JSplitPane)c;
			sp.setBorder(null);
			if (sp.getLeftComponent() instanceof JSplitPane)
				recursivelyRemoveSplitPaneBorders(sp.getLeftComponent());
			if (sp.getRightComponent() instanceof JSplitPane)
				recursivelyRemoveSplitPaneBorders(sp.getRightComponent());
			}
		}

	public void addRuntimePropertyListener(RuntimePropertyListener l) {
		mRPListener.add(l);
		}

	public void removeRuntimePropertyListener(RuntimePropertyListener l) {
		mRPListener.remove(l);
		}

	public void fireRuntimePropertyChanged(RuntimePropertyEvent e) {
		for (RuntimePropertyListener l:mRPListener)
			l.runtimePropertyChanged(e);
		}

	public DEMainPane getMainPane() {
		return mTabbedMainViews;
		}

	public DEDetailPane getDetailPane() {
		return mTabbedDetailViews;
		}

	public DEPruningPanel getPruningPanel() {
		return mPruningPanel;
		}

	public DECompoundTableModel getTableModel() {
		return mTableModel;
		}

	public JPopupMenu createPopupMenu(CompoundRecord record, CompoundTableView source, int selectedColumn, boolean isCtrlDown) {
        JPopupMenu popup = new DERowDetailPopupMenu(mTabbedMainViews, record, mPruningPanel, source, mDatabaseActions, selectedColumn, isCtrlDown);
        return (popup.getComponentCount() == 0) ? null : popup;
        }

	public double getMainSplitting() {
		return (double)mMainSplitPane.getDividerLocation() / (double)(mMainSplitPane.getWidth() - mMainSplitPane.getDividerSize());
		}

	public double getRightSplitting() {
		return (double)mRightSplitPane.getDividerLocation() / (double)(mRightSplitPane.getHeight() - mRightSplitPane.getDividerSize());
		}

	public void setMainSplitting(double l) {
		mMainSplitPane.setDividerLocation(Math.max(0.0, Math.min(1.0, l)));
		}

	public void setRightSplitting(double l) {
		mRightSplitPane.setDividerLocation(Math.max(0.0, Math.min(1.0, l)));
		}
	}