package com.actelion.research.gui.form;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.StereoMolecule;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.*;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.render.MoleculeArchitect;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class JFXConformerPanel extends JFXPanel {
	public static final double CAVITY_CROP_DISTANCE = 10.0;

	private V3DScene mScene;
	private V3DMolecule mOverlayMol;
	private V3DPopupMenuController mController;

	public JFXConformerPanel(boolean withSidePanel, boolean synchronousRotation, boolean allowEditing) {
		this(withSidePanel, 512, 384, synchronousRotation, allowEditing);
	}

	public JFXConformerPanel(boolean withSidePanel, int width, int height, boolean synchronousRotation, boolean allowEditing) {
		super();
		Platform.runLater(() -> {
			Scene scene;

			EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
			if (allowEditing)
				settings.add(V3DScene.ViewerSettings.EDITING);

			if (withSidePanel) {
				V3DSceneWithSidePane sceneWithSidePanel = new V3DSceneWithSidePane(width, height, settings);
//				sceneWithSidePanel.getMoleculePanel().initialize(false);
				mScene = sceneWithSidePanel.getScene3D();
				mScene.setIndividualRotationModus(synchronousRotation);
				scene = new Scene(sceneWithSidePanel, width, height, true, SceneAntialiasing.BALANCED);
			}
			else {
				mScene = new V3DScene(new Group(), width, height, settings);
				mScene.setIndividualRotationModus(synchronousRotation);
				V3DSceneWithSelection sws = new V3DSceneWithSelection(mScene);
				scene = new Scene(sws, width, height, true, SceneAntialiasing.BALANCED);
			}

			String css = getClass().getResource("/resources/molviewer.css").toExternalForm();
			scene.getStylesheets().add(css);
			mScene.widthProperty().bind(scene.widthProperty());
			mScene.heightProperty().bind(scene.heightProperty());
			mScene.setPopupMenuController(mController);
			setScene(scene);
		} );
	}

	public V3DScene getV3DScene() {
		return mScene;
	}

	/**
	 * Must be called directly after instantiation
	 * @param controller
	 */
	public void setPopupMenuController(V3DPopupMenuController controller) {
		mController = controller;
	}

	// this fixes an issue, where the JFXPanel, if not in focus does not properly handle popup menus
	// This is supposed to be fixed in Java9. Thus remove this when moving to Java9 or later. TODO
	@Override
	protected void processMouseEvent(MouseEvent e) {
		try {
			if ((e.getID() == MouseEvent.MOUSE_PRESSED)&& (e.getButton() != MouseEvent.BUTTON1)) {
				if (!hasFocus()) {
					requestFocus();
					AppContext context = SunToolkit.targetToAppContext(this);
					if (context != null) {
						SunToolkit.postEvent(context, e);
					}
				}
			}
		} catch (Exception ex) {}
		super.processMouseEvent(e);
	}

	/**
	 * Removes all except the reference molecule from the scene
	 */
	public void clear() {
		Platform.runLater(() -> {
			List<V3DMolecule> fxmols = mScene.getMolsInScene();
			for (V3DMolecule fxmol:fxmols) {
				if (fxmol != mOverlayMol) {
					mScene.removeMeasurements(fxmol);
					mScene.delete(fxmol);
				}
			}
		} );
	}

	public void optimizeView() {
		Platform.runLater(() -> mScene.optimizeView() );
	}

	@Override
	public void setBackground(java.awt.Color bg) {
		Platform.runLater(() -> {
			int r = bg.getRed();
			int g = bg.getGreen();
			int b = bg.getBlue();
			mScene.setFill(Color.rgb(r, g, b, 1));
		} );
	}

	public void setConformerSplitting(double value) {
		Platform.runLater(() -> {
			int count = 0;
			double molSize = 0;
			for (Node node:mScene.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					molSize = Math.max(molSize, 2 * Math.sqrt(((V3DMolecule)node).getMolecule().getAllAtoms()));
					count++;
				}
			}

			double maxLineWidth = 0.01 + molSize * Math.max(2.0, Math.round(1.2 * Math.sqrt(count)));
			int maxPerLine = (value == 0) ? count : (int)(1.0 + (maxLineWidth - molSize) / (value * molSize));
			int lineCount = (count == 0) ? 0 : 1 + (count - 1) / maxPerLine;
			int countPerLine = (count == 0) ? 0 : 1 + (count - 1) / lineCount;

			double x = 0.5;
			double y = 0.5;
			for (Node node:mScene.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					node.setTranslateX((x-(double)countPerLine/2) * molSize * value);
					node.setTranslateY((y-(double)lineCount/2) * molSize);
					x += 1;
					if (x > countPerLine) {
						x = 0.5;
						y += 1.0;
					}
				}
			}
		} );
	}

	public void setConollySurfaceMode(int mode) {
		Platform.runLater(() -> {
			for (Node node:mScene.getWorld().getAllChildren())
				if (node instanceof V3DMolecule)
					((V3DMolecule)node).setSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY, V3DMolecule.SurfaceMode.values()[mode]);
		} );
	}

	public StereoMolecule getOverlayMolecule() {
		return mOverlayMol == null ? null : mOverlayMol.getMolecule();
	}

	/**
	 * Adds the given small molecule into the scene.
	 * @param mol
	 */
	public void setOverlayMolecule(StereoMolecule mol) {
		Platform.runLater(() -> {
			mOverlayMol = new V3DMolecule(mol, 0, V3DMolecule.MoleculeRole.LIGAND);
			mScene.addMolecule(mOverlayMol);
			} );
		}

	/**
	 * Adds the given cropped protein cavity and its surface into the scene. If the natural ligand is given, then
	 * the ligand atom coordinates are used to determine the cavity surface in the ligand's vicinity.
	 * @param cavity
	 * @param ligand
	 */
	public void setProteinCavity(StereoMolecule cavity, StereoMolecule ligand) {
		Platform.runLater(() -> {
			if (ligand != null)
				markAtomsInCropDistance(cavity, ligand, calculateCOG(ligand));

			mOverlayMol = new V3DMolecule(cavity, MoleculeArchitect.ConstructionMode.WIRES, 0, V3DMolecule.MoleculeRole.MACROMOLECULE);
			mOverlayMol.setColor(Color.LIGHTGRAY);
			mOverlayMol.setSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY, V3DMolecule.SurfaceMode.FILLED);
			mOverlayMol.setSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS);

			mScene.addMolecule(mOverlayMol);
			} );
		}

	public static Coordinates calculateCOG(StereoMolecule mol) {
		Coordinates cog = new Coordinates(mol.getCoordinates(0));
		for (int i=1; i<mol.getAllAtoms(); i++)
			cog.add(mol.getCoordinates(i));
		cog.scale(1.0/mol.getAllAtoms());
		return cog;
		}

	public static StereoMolecule cropProtein(StereoMolecule protein, StereoMolecule ligand, Coordinates ligandCOG) {
		double maxDistance = 0;
		for (int i=0; i<ligand.getAllAtoms(); i++)
			maxDistance = Math.max(maxDistance, ligandCOG.distance(ligand.getCoordinates(i)));

		double cropDistance = JFXConformerPanel.CAVITY_CROP_DISTANCE;
		maxDistance += cropDistance;

		// mark all protein atoms within crop distance
		boolean[] isInCropRadius = new boolean[protein.getAllAtoms()];
		for (int i=0; i<protein.getAllAtoms(); i++) {
			Coordinates pc = protein.getCoordinates(i);
			if (Math.abs(pc.x - ligandCOG.x) < maxDistance
			 && Math.abs(pc.y - ligandCOG.y) < maxDistance
			 && Math.abs(pc.z - ligandCOG.z) < maxDistance
			 && pc.distance(ligandCOG) < maxDistance) {
				for (int j=0; j<ligand.getAllAtoms(); j++) {
					if (pc.distance(ligand.getCoordinates(j)) < cropDistance) {
						isInCropRadius[i] = true;
						break;
						}
					}
				}
			}

		int[] uncroppedNeighbours = new int[protein.getAllAtoms()];
		for (int i=0; i<protein.getAllBonds(); i++) {
			int atom1 = protein.getBondAtom(0, i);
			int atom2 = protein.getBondAtom(1, i);
			if (isInCropRadius[atom1])
				uncroppedNeighbours[atom2]++;
			if (isInCropRadius[atom2])
				uncroppedNeighbours[atom1]++;
			}
		for (int i=0; i<protein.getAllAtoms(); i++) {
			if (isInCropRadius[i] && uncroppedNeighbours[i] == 0)
				isInCropRadius[i] = false;
			else if (!isInCropRadius[i] && uncroppedNeighbours[i] != 0 && protein.getAllConnAtoms(i) == uncroppedNeighbours[i])
				isInCropRadius[i] = true;
			}

		// set atomicNo=0 for outside crop distance atoms, which are connected to inside atoms
		// and determine for every uncropped atom, whether it has an uncropped neighbour
		for (int i=0; i<protein.getAllBonds(); i++) {
			int atom1 = protein.getBondAtom(0, i);
			int atom2 = protein.getBondAtom(1, i);
			if (isInCropRadius[atom1] && protein.getAtomicNo(atom1) != 0 && !isInCropRadius[atom2]) {
				protein.setAtomicNo(atom2, 0);
				isInCropRadius[atom2] = true;
				}
			else if (isInCropRadius[atom2] && protein.getAtomicNo(atom2) != 0 && !isInCropRadius[atom1]) {
				protein.setAtomicNo(atom1, 0);
				isInCropRadius[atom1] = true;
				}
			}

		StereoMolecule croppedProtein = new StereoMolecule();
		protein.copyMoleculeByAtoms(croppedProtein, isInCropRadius, false, null);
		croppedProtein.setFragment(false);
		return croppedProtein;
		}

	/**
	 * If the panel is supposed to show a protein cavity created by cropping a larger protein,
	 * and if the cavity surface shall be shown in the area of the natural ligand, then this
	 * method can be used to mark all cavity atoms that shall covered by the surface.
	 * @param cavity
	 * @param ligand
	 */
	public static void markAtomsInCropDistance(StereoMolecule cavity, StereoMolecule ligand, Coordinates ligandCOG) {
		double maxDistance = 0;
		for (int i=0; i<ligand.getAllAtoms(); i++)
			maxDistance = Math.max(maxDistance, ligandCOG.distance(ligand.getCoordinates(i)));

		double markDistance = JFXConformerPanel.CAVITY_CROP_DISTANCE - 3.2;
		maxDistance += markDistance;

		for (int i=0; i<cavity.getAllAtoms(); i++)
			cavity.setAtomMarker(i, true);

		// reset mark for all cavity atoms near any ligand atom
		for (int i=0; i<cavity.getAllAtoms(); i++) {
			Coordinates pc = cavity.getCoordinates(i);
			if (Math.abs(pc.x - ligandCOG.x) < maxDistance
			 && Math.abs(pc.y - ligandCOG.y) < maxDistance
			 && Math.abs(pc.z - ligandCOG.z) < maxDistance
			 && pc.distance(ligandCOG) < maxDistance) {
				for (int j=0; j<ligand.getAllAtoms(); j++) {
					if (pc.distance(ligand.getCoordinates(j)) < markDistance) {
						cavity.setAtomMarker(i, false);
						break;
						}
					}
				}
			}
		}

	public void addMolecule(StereoMolecule mol, Color color, Point3D centerOfRotation) {
		Platform.runLater(() -> {
			V3DMolecule fxmol = new V3DMolecule(mol);
			if (color != null)
				fxmol.setColor(color);
			fxmol.setSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY, SurfaceMesh.SURFACE_COLOR_INHERIT);
			fxmol.setCenterOfRotation(centerOfRotation);
			mScene.addMolecule(fxmol);
		} );
	}

	public ArrayList<StereoMolecule> getMolecules(V3DMolecule.MoleculeRole role) {
		final ArrayList<StereoMolecule> conformerList = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(1);

		Platform.runLater(() -> {
			for (Node node:mScene.getWorld().getChildren())
				if (node instanceof V3DMolecule
				 && role == null || ((V3DMolecule)node).getRole() == role)
					conformerList.add(((V3DMolecule)node).getMolecule());
			latch.countDown();
		} );

		try { latch.await(); } catch (InterruptedException ie) {}
		return conformerList;
	}

	public WritableImage getContentImage() {
		final ArrayList<WritableImage> imageList = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(1);

		Platform.runLater(() -> {
			WritableImage image = mScene.snapshot(null, null);
			imageList.add(image);
			latch.countDown();
		} );

		try { latch.await(); } catch (InterruptedException ie) {}

		return imageList.isEmpty() ? null : imageList.get(0);
	}
}
