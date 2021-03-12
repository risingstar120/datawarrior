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

import com.actelion.research.chem.*;
import com.actelion.research.chem.alignment3d.PheSAAlignmentOptimizer;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.task.table.DETaskSetColumnProperties;
import com.actelion.research.gui.*;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.form.*;
import com.actelion.research.table.CompoundTableChemistryCellRenderer;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.JDetailTable;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.util.ArrayUtils;
import javafx.geometry.Point3D;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.paint.Color;
import org.openmolecules.fx.viewer3d.V3DPopupMenuController;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DEDetailPane extends JMultiPanelView implements HighlightListener,CompoundTableListener,CompoundTableColorHandler.ColorListener {
	private static final long serialVersionUID = 0x20060904;

	private static final String TYPE_STRUCTURE_2D = "Structure";
	private static final String TYPE_STRUCTURE_3D = "3D-Structure";
	private static final String TYPE_REACTION = "Reaction";
	private static final String TYPE_ROW_DATA = "Data";
	private static final String TYPE_IMAGE = "Image";
	protected static final String TYPE_DETAIL = "Detail";

	private Color REFERENCE_COLOR = Color.WHITE;

	private DEFrame mFrame;
	private CompoundTableModel mTableModel;
	private CompoundRecord mCurrentRecord;
	private DetailTableModel mDetailModel;
	private JDetailTable mDetailTable;
	private ArrayList<DetailViewInfo> mDetailViewList;

	public DEDetailPane(DEFrame frame, CompoundTableModel tableModel) {
		super();
		mFrame = frame;
		mTableModel = tableModel;
		mTableModel.addCompoundTableListener(this);
		mTableModel.addHighlightListener(this);

		setMinimumSize(new Dimension(100, 100));
		setPreferredSize(new Dimension(100, 100));

		mDetailModel = new DetailTableModel(mTableModel);
		mDetailTable = new JDetailTable(mDetailModel);
		Font tableFont = UIManager.getFont("Table.font");
		mDetailTable.setFont(tableFont.deriveFont(Font.PLAIN, tableFont.getSize() * 11 / 12));
		mDetailTable.putClientProperty("Quaqua.Table.style", "striped");

		// to eliminate the disabled default action of the JTable when typing menu-V
		mDetailTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mDetailTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, TYPE_ROW_DATA);

		mDetailViewList = new ArrayList<DetailViewInfo>();
	}

	public void setColorHandler(CompoundTableColorHandler colorHandler) {
		mDetailTable.setColorHandler(colorHandler);
		colorHandler.addColorListener(this);
	}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cAddColumns) {
			int firstNewView = mDetailViewList.size();
			addColumnDetailViews(e.getColumn());

			for (int i = firstNewView; i < mDetailViewList.size(); i++)
				updateDetailView(mDetailViewList.get(i));
		} else if (e.getType() == CompoundTableEvent.cNewTable) {
			mCurrentRecord = null;

			for (DetailViewInfo viewInfo : mDetailViewList)
				remove(viewInfo.view);
			mDetailViewList.clear();

			addColumnDetailViews(0);
		} else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			for (DetailViewInfo viewInfo : mDetailViewList) {
				if (e.getColumn() == viewInfo.column) {
					String title = createDetailTitle(viewInfo);
					if (title != null) {
						for (int i = 0; i < getViewCount(); i++) {
							if (getView(i) == viewInfo.view) {
								setTitle(i, title);
								break;
							}
						}
					}
				}
			}
		} else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			for (DetailViewInfo viewInfo : mDetailViewList)
				if (e.getColumn() == viewInfo.column)
					updateDetailView(viewInfo);
		} else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			for (int i = mDetailViewList.size() - 1; i >= 0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				viewInfo.column = e.getMapping()[viewInfo.column];

				// for STRUCTURE(_3D) types viewInfo.detail contains the coordinate column
				if (viewInfo.type.equals(TYPE_STRUCTURE_2D) || viewInfo.type.equals(TYPE_REACTION)) {
					if (viewInfo.detail != -1)  // 2D-coords may be generated on-the-fly
						viewInfo.detail = e.getMapping()[viewInfo.detail];
				}
				if (viewInfo.type.equals(TYPE_STRUCTURE_3D)) {
					viewInfo.detail = e.getMapping()[viewInfo.detail];
					if (viewInfo.detail == -1)  // remove view if 3D-coords missing
						viewInfo.column = -1;
				}

				if (viewInfo.column == -1) {
					remove(viewInfo.view);
					mDetailViewList.remove(i);
				}
			}
		} else if (e.getType() == CompoundTableEvent.cAddColumnDetails) {
			int column = e.getColumn();
			for (int detail = 0; detail < mTableModel.getColumnDetailCount(column); detail++) {
				boolean found = false;
				for (int i = 0; i < mDetailViewList.size(); i++) {
					DetailViewInfo viewInfo = mDetailViewList.get(i);
					if (viewInfo.type.equals(TYPE_DETAIL) && e.getColumn() == viewInfo.column && viewInfo.detail == detail) {
						found = true;
						break;
					}
				}
				if (!found)
					addResultDetailView(column, detail);
			}
		} else if (e.getType() == CompoundTableEvent.cRemoveColumnDetails) {
			for (int i = mDetailViewList.size() - 1; i >= 0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				if (viewInfo.type.equals(TYPE_DETAIL) && e.getColumn() == viewInfo.column) {
					viewInfo.detail = e.getMapping()[viewInfo.detail];
					if (viewInfo.detail == -1) {
						remove(viewInfo.view);
						mDetailViewList.remove(i);
					}
				}
			}
		}
	}

	public void colorChanged(int column, int type, VisualizationColor color) {
		for (DetailViewInfo viewInfo : mDetailViewList) {
			if (viewInfo.column == column) {
				if (viewInfo.type.equals(TYPE_STRUCTURE_2D) || viewInfo.type.equals(TYPE_REACTION)) {
					updateDetailView(viewInfo);
				}
			}
		}
		mDetailTable.repaint();
	}

	public CompoundTableModel getTableModel() {
		return mTableModel;
	}

	protected void addColumnDetailViews(int firstColumn) {
		for (int column = firstColumn; column < mTableModel.getTotalColumnCount(); column++) {
			String columnName = mTableModel.getColumnTitleNoAlias(column);
			String specialType = mTableModel.getColumnSpecialType(column);
			if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
				int coordinateColumn = mTableModel.getChildColumn(column, CompoundTableModel.cColumnType2DCoordinates);
				JStructureView view = new JStructureView(DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
				view.setDisplayMode(AbstractDepictor.cDModeHiliteAllQueryFeatures | AbstractDepictor.cDModeSuppressChiralText);
				view.setClipboardHandler(new ClipboardHandler());
				addColumnDetailView(view, column, coordinateColumn, TYPE_STRUCTURE_2D, mTableModel.getColumnTitle(column));
				continue;
			}
			if (CompoundTableModel.cColumnTypeRXNCode.equals(specialType)) {
				int coordinateColumn = mTableModel.getChildColumn(column, CompoundTableModel.cColumnType2DCoordinates);
				JChemistryView view = new JChemistryView(ExtendedDepictor.TYPE_REACTION);
				addColumnDetailView(view, column, coordinateColumn, TYPE_REACTION, mTableModel.getColumnTitle(column));
				continue;
			}
			if (CompoundTableModel.cColumnType3DCoordinates.equals(specialType)) {
				final JFXConformerPanel view = new JFXConformerPanel(false, false, false);
				view.setBackground(new java.awt.Color(24, 24, 96));
				String refmol = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertySuperposeMolecule);
				if (refmol != null)
					view.setReferenceMolecule(new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(refmol));
				DetailViewInfo viewInfo = addColumnDetailView(view, mTableModel.getParentColumn(column), column, TYPE_STRUCTURE_3D, mTableModel.getColumnTitle(column));
				view.setPopupMenuController(new Detail3DViewController(viewInfo));
				continue;
			}
			if (columnName.equalsIgnoreCase("imagefilename")
					|| columnName.equals("#Image#")
					|| mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath) != null) {
				boolean useThumbNail = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyUseThumbNail).equals("true");
				String imagePath = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath);
				if (imagePath == null)
					imagePath = FileHelper.getCurrentDirectory() + File.separator + "images" + File.separator;
				JImagePanel view = new JImagePanel(imagePath, useThumbNail);
				String viewName = (columnName.equalsIgnoreCase("imagefilename")
						|| columnName.equals("#Image#")) ? TYPE_IMAGE : mTableModel.getColumnTitle(column);
				addColumnDetailView(view, column, -1, TYPE_IMAGE, viewName);
				continue;
			}
			for (int detail = 0; detail < mTableModel.getColumnDetailCount(column); detail++) {
				addResultDetailView(column, detail);
			}
		}
	}

	private String createDetailTitle(DetailViewInfo info) {
		if (info.type.equals(TYPE_STRUCTURE_2D) || info.type.equals(TYPE_REACTION))
			return mTableModel.getColumnTitle(info.column);
		if (info.type.equals(TYPE_STRUCTURE_3D))
			return mTableModel.getColumnTitle(info.detail);
		if (info.type.equals(TYPE_IMAGE)) {
			String columnName = mTableModel.getColumnTitleNoAlias(info.column);
			return (columnName.equalsIgnoreCase("imagefilename")
					|| columnName.equals("#Image#")) ? TYPE_IMAGE : mTableModel.getColumnTitle(info.column);
		}
		if (info.type.equals(TYPE_DETAIL))
			return mTableModel.getColumnDetailName(info.column, info.detail) + " (" + mTableModel.getColumnTitle(info.column) + ")";
		return null;
	}

	private void addResultDetailView(int column, int detail) {
		String mimetype = mTableModel.getColumnDetailType(column, detail);
		JResultDetailView view = createResultDetailView(column, detail, mimetype);
		if (view != null)
			addColumnDetailView(view, column, detail, TYPE_DETAIL, mTableModel.getColumnDetailName(column, detail)
					+ " (" + mTableModel.getColumnTitle(column) + ")");
	}

	protected JResultDetailView createResultDetailView(int column, int detail, String mimetype) {
		CompoundTableDetailSpecification spec = new CompoundTableDetailSpecification(getTableModel(), column, detail);

		if (mimetype.equals(JHTMLDetailView.TYPE_TEXT_PLAIN)
				|| mimetype.equals(JHTMLDetailView.TYPE_TEXT_HTML))
			return new JHTMLDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec, mimetype);

		if (mimetype.equals(JImageDetailView.TYPE_IMAGE_JPEG)
				|| mimetype.equals(JImageDetailView.TYPE_IMAGE_GIF)
				|| mimetype.equals(JImageDetailView.TYPE_IMAGE_PNG))
			return new JImageDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec);

		if (mimetype.equals(JSVGDetailView.TYPE_IMAGE_SVG))
			return new JSVGDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec);

		return null;
	}

	protected DetailViewInfo addColumnDetailView(JComponent view, int column, int detail, String type, String title) {
		DetailViewInfo viewInfo = new DetailViewInfo(view, column, detail, type);
		mDetailViewList.add(viewInfo);
		add(view, title);
		return viewInfo;
	}

	public void highlightChanged(CompoundRecord record) {
		if (mCurrentRecord != record && record != null) {
			mCurrentRecord = record;

			for (DetailViewInfo viewInfo : mDetailViewList)
				updateDetailView(viewInfo);
		}
	}

	private void updateDetailView(DetailViewInfo viewInfo) {
		if (viewInfo.type.equals(TYPE_STRUCTURE_2D)) {
			StereoMolecule mol = null;
			StereoMolecule displayMol = null;
			if (mCurrentRecord != null) {
				byte[] idcode = (byte[]) mCurrentRecord.getData(viewInfo.column);
				if (idcode != null) {
					int coordinateColumn = (viewInfo.column == -1) ? -1
							: mTableModel.getChildColumn(viewInfo.column, CompoundTableModel.cColumnType2DCoordinates);
					if ((coordinateColumn != -1 && mCurrentRecord.getData(coordinateColumn) != null)
							|| new IDCodeParser().getAtomCount(idcode, 0) <= CompoundTableChemistryCellRenderer.ON_THE_FLY_COORD_MAX_ATOMS) {
						mol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
						displayMol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_ALL, null);
					}
				}
			}
			((JStructureView) viewInfo.view).structureChanged(mol, displayMol);
		}
		if (viewInfo.type.equals(TYPE_REACTION)) {
			Reaction rxn = null;
			if (mCurrentRecord != null)
				rxn = mTableModel.getChemicalReaction(mCurrentRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_ALL);
			((JChemistryView) viewInfo.view).setContent(rxn);
		} else if (viewInfo.type.equals(TYPE_STRUCTURE_3D)) {
			boolean isSuperpose = CompoundTableConstants.cSuperposeValueReferenceRow.equals(mTableModel.getColumnProperty(viewInfo.detail, CompoundTableConstants.cColumnPropertySuperpose));
			boolean isAlign = CompoundTableConstants.cSuperposeAlignValueShape.equals(mTableModel.getColumnProperty(viewInfo.detail, CompoundTableConstants.cColumnPropertySuperposeAlign));
			update3DView(viewInfo, isSuperpose, isAlign);
		} else if (viewInfo.type.equals(TYPE_IMAGE)) {
			((JImagePanel) viewInfo.view).setFileName((mCurrentRecord == null) ? null
					: mTableModel.encodeData(mCurrentRecord, viewInfo.column));
		} else if (viewInfo.type.equals(TYPE_DETAIL)) {
			if (mCurrentRecord == null) {
				((JResultDetailView) viewInfo.view).setReferences(null);
			} else {
				String[][] reference = mCurrentRecord.getDetailReferences(viewInfo.column);
				((JResultDetailView) viewInfo.view).setReferences(reference == null
						|| reference.length <= viewInfo.detail ?
						null : reference[viewInfo.detail]);
			}
		}
	}

	private void update3DView(DetailViewInfo viewInfo, boolean isSuperpose, boolean isAlign) {
		((JFXConformerPanel) viewInfo.view).clear();

		StereoMolecule[] rowMol = null;
		if (mCurrentRecord != null)
			rowMol = getConformers(mCurrentRecord, true, viewInfo);

		StereoMolecule[] refMol = null;
		if (isSuperpose && mTableModel.getActiveRow() != null && mTableModel.getActiveRow() != mCurrentRecord)
			refMol = getConformers(mTableModel.getActiveRow(), false, viewInfo);

		boolean conformersAdded = false;
		if (rowMol != null) {
			StereoMolecule best = null;
			if (isAlign && refMol != null) {
				double maxFit = 0;
				for (int i = 0; i < rowMol.length; i++) {
					double fit = PheSAAlignmentOptimizer.alignTwoMolsInPlace(refMol[0], rowMol[i]);
					if (fit > maxFit) {
						maxFit = fit;
						best = rowMol[i];
					}
				}

				rowMol = new StereoMolecule[1];
				rowMol[0] = best;
			}

			conformersAdded |= addConformers(rowMol, null, viewInfo);
		}

		if (refMol != null)
			conformersAdded |= addConformers(refMol, REFERENCE_COLOR, viewInfo);

		if (conformersAdded)
			((JFXConformerPanel) viewInfo.view).optimizeView();
	}

	private StereoMolecule[] getConformers(CompoundRecord record, boolean allowMultiple, DetailViewInfo viewInfo) {
		byte[] idcode = (byte[]) record.getData(viewInfo.column);
		byte[] coords = (byte[]) record.getData(viewInfo.detail);
		if (idcode != null && coords != null) {
			int count = 1;
			int index = ArrayUtils.indexOf(coords, (byte) 32);
			if (index != -1 && allowMultiple) {
				count++;
				for (int i = index + 1; i < coords.length; i++)
					if (coords[i] == (byte) 32)
						count++;
				index = -1;
			}
			StereoMolecule[] mol = new StereoMolecule[count];
			for (int i = 0; i < count; i++) {
				mol[i] = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, coords, 0, index + 1);
				index = ArrayUtils.indexOf(coords, (byte) 32, index + 1);
			}
			return mol;
		}
		return null;
	}

	private boolean addConformers(StereoMolecule[] mol, Color color, DetailViewInfo viewInfo) {
		if (mol == null || mol.length == 0)
			return false;

		if (mol.length == 1) {
			((JFXConformerPanel) viewInfo.view).addMolecule(mol[0], color, null);
		} else {
			for (int i = 0; i < mol.length; i++) {
				Point3D cor = new Point3D(0, 0, 0);
				Color c = (color != null) ? color : Color.hsb(360f * i / mol.length, 0.75, 0.6);
				((JFXConformerPanel) viewInfo.view).addMolecule(mol[i], c, cor);
			}
		}
		return true;
	}

	private void setSuperposeMode(DetailViewInfo viewInfo, boolean isSuperpose, boolean isShapeAlign) {
		System.out.println("setSuperposeMode(isSuperpose:" + isSuperpose + ", isShapeAlign:" + isShapeAlign + ")");
		SwingUtilities.invokeLater(() -> {
			HashMap<String, String> map = new HashMap<>();
			map.put(CompoundTableConstants.cColumnPropertySuperpose, isSuperpose ? CompoundTableConstants.cSuperposeValueReferenceRow : null);
			map.put(CompoundTableConstants.cColumnPropertySuperposeAlign, isShapeAlign ? CompoundTableConstants.cSuperposeAlignValueShape : null);
			new DETaskSetColumnProperties(mFrame, viewInfo.detail, map, false).defineAndRun();
			update3DView(viewInfo, isSuperpose, isShapeAlign);
		});
	}

	class Detail3DViewController implements V3DPopupMenuController {
		public DetailViewInfo viewInfo;

		public Detail3DViewController(DetailViewInfo viewInfo) {
			this.viewInfo = viewInfo;
		}

		@Override
		public void addExternalMenuItems(ContextMenu popup, int type) {
			if (type == V3DPopupMenuController.TYPE_VIEW) {
				boolean isSuperpose = CompoundTableConstants.cSuperposeValueReferenceRow.equals(mTableModel.getColumnProperty(viewInfo.detail, CompoundTableConstants.cColumnPropertySuperpose));
				boolean isShapeAlign = CompoundTableConstants.cSuperposeAlignValueShape.equals(mTableModel.getColumnProperty(viewInfo.detail, CompoundTableConstants.cColumnPropertySuperposeAlign));

				javafx.scene.control.CheckMenuItem itemSuperpose = new CheckMenuItem("Superpose Reference Row");
				itemSuperpose.setSelected(isSuperpose);
				itemSuperpose.setOnAction(e -> setSuperposeMode(viewInfo, !isSuperpose, isShapeAlign));

				javafx.scene.control.CheckMenuItem itemAlignShape = new CheckMenuItem("Align Shapes");
				itemAlignShape.setSelected(isShapeAlign);
				itemAlignShape.setDisable(!isSuperpose);
				itemAlignShape.setOnAction(e -> setSuperposeMode(viewInfo, isSuperpose, !isShapeAlign));

				popup.getItems().add(itemSuperpose);
				popup.getItems().add(itemAlignShape);
				popup.getItems().add(new SeparatorMenuItem());
			}
		}
	}

	class DetailViewInfo {
		public JComponent view;
		public int column, detail;
		public String type;

		public DetailViewInfo(JComponent view, int column, int detail, String type) {
			this.view = view;
			this.column = column;   // is idcode column in case of STRUCTURE(_3D)
			this.detail = detail;   // is coordinate column in case of STRUCTURE(_3D)
			this.type = type;
		}
	}
}