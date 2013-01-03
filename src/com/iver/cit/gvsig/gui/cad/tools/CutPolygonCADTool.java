/*
 * Copyright 2008 Deputación Provincial de A Coruña
 * Copyright 2009 Deputación Provincial de Pontevedra
 * Copyright 2010 CartoLab, Universidad de A Coruña
 *
 * This file is part of openCADTools, developed by the Cartography
 * Engineering Laboratory of the University of A Coruña (CartoLab).
 * http://www.cartolab.es
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,USA.
 */

package com.iver.cit.gvsig.gui.cad.tools;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.values.Value;
import com.iver.andami.PluginServices;
import com.iver.andami.messages.NotificationManager;
import com.iver.cit.gvsig.CADExtension;
import com.iver.cit.gvsig.exceptions.validate.ValidateRowException;
import com.iver.cit.gvsig.fmap.ViewPort;
import com.iver.cit.gvsig.fmap.core.DefaultFeature;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.GeneralPathX;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.ShapeFactory;
import com.iver.cit.gvsig.fmap.edition.DefaultRowEdited;
import com.iver.cit.gvsig.fmap.edition.EditionEvent;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.VectorialEditableAdapter;
import com.iver.cit.gvsig.gui.cad.DefaultCADTool;
import com.iver.cit.gvsig.gui.cad.exception.CommandException;
import com.iver.cit.gvsig.gui.cad.tools.smc.CutPolygonCADToolContext;
import com.iver.cit.gvsig.layers.VectorialLayerEdited;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Tool to cut a polygon geometry on twice
 * 
 * @author Jose Ignacio Lamas [LBD]
 * @author Nacho Varela [Cartolab]
 * @author Pablo Sanxiao [CartoLab]
 */
public class CutPolygonCADTool extends CutRedigitalizeCommons {
    private static final String CUT_ACTION_COMMAND = "_cut_polygon";
    public static final String CUT_LISTENER_END_SECOND_POLYGON = "_cut_polygon_end";
    public static final String CUT_LISTENER_END_FIRST_POLYGON = "_cut_polygon_end_first_geom";
    public static final String CUT_LISTENER_DELETE_SECOND_POLYGON = "_cut_polygon_delete_second";

    private CutPolygonCADToolContext _fsm;

    private ArrayList<IGeometry> geomsArray = null;

    private Value[] values;

    @Override
    public void init() {
	// super.init();
	clear();
	_fsm = new CutPolygonCADToolContext(this);
	geomsArray = new ArrayList<IGeometry>();
    }

    @Override
    public void transition(double x, double y, InputEvent event) {
	_fsm.addPoint(x, y, event);
    }

    @Override
    public void transition(double d) {
	// _fsm.addValue(d);
    }

    @Override
    public void transition(String s) throws CommandException {
	if (!super.changeCommand(s)) {
	    _fsm.addOption(s);
	}
    }

    @Override
    public void transition(InputEvent event) {
	if (newPoints != null) {
	    _fsm.removePoint(event, newPoints.size());
	} else {
	    _fsm.removePoint(event, 0);
	}
    }

    @Override
    public void addPoint(double x, double y, InputEvent event) {

	// Check if the point inserted intersects the current geometry
	Geometry aux = ShapeFactory.createPoint2D(x, y).toJTSGeometry();
	if (selectedGeom == null
		|| !selectedGeom.toJTSGeometry().intersects(aux)) {
	    return;
	}
	newPoints.add(new Point2D.Double(x, y));
    }

    @Override
    public void addValue(double d) {
    }

    @Override
    public void addOption(String s) {
    }

    @Override
    public void drawOperation(Graphics g, ArrayList pointsList) {
	if (firstPoint == null || oldPoints == null) {
	    return;
	}
	Point2D pointAux = null;
	int sizePixels = 12;
	int half = sizePixels / 2;
	if (firstPoint != null) {
	    pointAux = CADExtension.getEditionManager().getMapControl()
		    .getViewPort().fromMapPoint(firstPoint);
	    drawPixel(pointAux, g, sizePixels, half, firstPoint);
	    g.drawString("C1", (int) pointAux.getX(),
		    (int) (pointAux.getY() - 10));

	    // Painting the line so that user choose the next point
	    if (secondPoint == null) {
		GeneralPathX gpx = new GeneralPathX();
		move_line_asc(0, oldPoints.size() - 1, gpx);
		gpx.closePath();
		drawGeometryAndHandlers(g, gpx);
	    }
	}
	if (secondPoint != null) {
	    pointAux = CADExtension.getEditionManager().getMapControl()
		    .getViewPort().fromMapPoint(secondPoint);
	    drawPixel(pointAux, g, sizePixels, half, secondPoint);
	    g.drawString("C2", (int) pointAux.getX(),
		    (int) (pointAux.getY() - 10));

	    // Painting the part of the line that we keep
	    GeneralPathX gpx = new GeneralPathX();
	    int firstCutIndex = firstPointIndex;
	    int secondCutIndex = secondPointIndex;
	    if (firstCutIndex > secondCutIndex) {
		int aux = firstCutIndex;
		firstCutIndex = secondCutIndex;
		secondCutIndex = aux;

		if (((((secondCutIndex - firstCutIndex) * 2) <= (oldPoints
			.size())) && (doShortPath))
			|| ((((secondCutIndex - firstCutIndex) * 2) > (oldPoints
				.size())) && (!doShortPath))) {

		    move_line_desc(oldPoints.size() - 1, secondCutIndex, gpx);
		    pointsDigitizedByTheUser(gpx);
		    snapperPoints(g, pointsList, gpx, 10);
		    line_desc(firstCutIndex, 0, pointsList, gpx);

		} else {
		    // We have to cover first the points between the second and
		    // the first
		    // index and then the introduced by the user
		    move_line_asc(firstCutIndex, secondCutIndex, gpx);
		    pointsDigitizedByTheUser(gpx);
		    snapperPoints(g, pointsList, gpx, 10);
		    gpx.closePath();
		}
	    } else {
		if (((((secondCutIndex - firstCutIndex) * 2) <= (oldPoints
			.size())) && (doShortPath))
			|| ((((secondCutIndex - firstCutIndex) * 2) > (oldPoints
				.size())) && (!doShortPath))) {

		    // We have to cover first the points between the second and
		    // the first index and then the introduced by the user
		    move_line_asc(0, firstCutIndex, gpx);
		    pointsDigitizedByTheUser(gpx);
		    snapperPoints(g, pointsList, gpx, 8);
		    line_asc(secondCutIndex, oldPoints.size(), oldPoints, gpx);

		} else {
		    move_line_desc(secondCutIndex, firstCutIndex, gpx);
		    pointsDigitizedByTheUser(gpx);
		    snapperPoints(g, pointsList, gpx, 10);

		    gpx.closePath();
		}
	    }

	    // Checking that the points are ordered
	    if (gpx.isCCW()) {
		gpx.flip();
	    }

	    drawGeometryAndHandlers(g, gpx);
	}
	// Cleaning the last point of the snappers
	cleanSnapper();
    }

    @Override
    public String getName() {
	return PluginServices.getText(this, "cut_polygon_");
    }

    @Override
    public String toString() {
	return CUT_ACTION_COMMAND;
    }

    @Override
    public boolean isApplicable(int shapeType) {
	switch (shapeType) {
	case FShape.POLYGON:
	case FShape.MULTI:
	    return true;
	}
	return false;
    }

    @Override
    public void drawOperation(Graphics g, double x, double y) {
	ArrayList<Double> lista = new ArrayList<Double>();
	lista.add(new Point2D.Double(x, y));
	drawOperation(g, lista);
    }

    /**
     * Storing the changes made to the geometry and ask user if wants to create
     * a new one, if it is possible, with the rest of the geometry. Also cut a
     * secondary geometry if so.
     */
    public void saveChanges() {

	if (selectedRow != null) {
	    int resp = JOptionPane.NO_OPTION;
	    resp = JOptionPane.showConfirmDialog(
		    (Component) PluginServices.getMainFrame(),
		    PluginServices.getText(this, "keep_remaining_feature"),
		    PluginServices.getText(this, "cut_polygon"),
		    JOptionPane.YES_NO_OPTION);

	    System.out.println("--->>> Salving changes in cutted geometry");
	    ((IFeature) selectedRow.getLinkedRow())
		    .setGeometry(getCuttedGeometry());
	    IGeometry baseGeometry = ((IFeature) selectedRow.getLinkedRow())
		    .getGeometry();
	    geomsArray.clear();
	    geomsArray.add(baseGeometry);
	    modifyFeature(selectedRow.getIndex(),
		    (IFeature) selectedRow.getLinkedRow());

	    if (resp == JOptionPane.YES_OPTION) {
		fireEndGeometry(CUT_LISTENER_END_FIRST_POLYGON);
		Value[] values = getParametrizableValues();
		IGeometry newGeometry = getRemainingGeometry();
		geomsArray.add(newGeometry);
		addGeometryWithParametrizedValues(newGeometry, values);
		fireEndGeometry(CutPolygonCADTool.CUT_LISTENER_END_SECOND_POLYGON);
	    } else {
		fireEndGeometry(CUT_LISTENER_DELETE_SECOND_POLYGON);
	    }

	} else {

	    // TODO Delete when we check that this branch never is called
	    System.out
		    .println("%$%ï¿½$/$ï¿½/%$ï¿½/   CREO QUE ESTA RAMA NUNCA ES LLAMADA   ");
	    System.out
		    .println("%$%ï¿½$/$ï¿½/%$ï¿½/   CREO QUE ESTA RAMA NUNCA ES LLAMADA   ");
	    System.out
		    .println("%$%ï¿½$/$ï¿½/%$ï¿½/   CREO QUE ESTA RAMA NUNCA ES LLAMADA   ");
	    System.out
		    .println("%$%ï¿½$/$ï¿½/%$ï¿½/   CREO QUE ESTA RAMA NUNCA ES LLAMADA   ");
	    System.out
		    .println("%$%ï¿½$/$ï¿½/%$ï¿½/   CREO QUE ESTA RAMA NUNCA ES LLAMADA   ");

	    int resp = JOptionPane.NO_OPTION;
	    resp = JOptionPane
		    .showConfirmDialog((Component) PluginServices
			    .getMainFrame(), PluginServices.getText(this,
			    "cortar_linea_restante_fuera"), PluginServices
			    .getText(this, "cut_polygon"),
			    JOptionPane.YES_NO_OPTION);
	    if (resp != JOptionPane.YES_OPTION) { // CANCEL DELETE

	    } else {
		// Storing the result geometry and discard the rest
		System.out
			.println("--->>> Salvando los cambios en la geometria");
		((IFeature) selectedRow.getLinkedRow())
			.setGeometry(getCuttedGeometry());
		modifyFeature(selectedRow.getIndex(),
			(IFeature) selectedRow.getLinkedRow());
	    }
	}
    }

    public void setParametrizableValues(Value[] newValues) {
	values = newValues;
    }

    private Value[] getParametrizableValues() {
	if (values != null) {
	    return values;
	} else {
	    values = selectedRow.getAttributes().clone();
	    return values;
	}
    }

    public void addNewElement(IGeometry geometry, IRowEdited row) {
	VectorialLayerEdited vle = getVLE();
	VectorialEditableAdapter vea = getVLE().getVEA();

	int numAttr;
	try {
	    numAttr = vea.getRecordset().getFieldCount();

	    Value[] values = new Value[numAttr];
	    values = row.getAttributes();

	    String newFID;

	    newFID = vea.getNewFID();
	    DefaultFeature df = new DefaultFeature(geometry, values, newFID);
	    int index = vea.addRow(df, getName(), EditionEvent.GRAPHIC);
	    clearSelection();
	    ArrayList selectedRow = vle.getSelectedRow();

	    ViewPort vp = vle.getLayer().getMapContext().getViewPort();
	    BufferedImage selectionImage = new BufferedImage(
		    vp.getImageWidth(), vp.getImageHeight(),
		    BufferedImage.TYPE_INT_ARGB);
	    Graphics2D gs = selectionImage.createGraphics();
	    int inversedIndex = vea.getInversedIndex(index);
	    selectedRow.add(new DefaultRowEdited(df, IRowEdited.STATUS_ADDED,
		    inversedIndex));
	    vea.getSelection().set(inversedIndex);
	    IGeometry geom = df.getGeometry();
	    geom.cloneGeometry().draw(gs, vp, DefaultCADTool.selectionSymbol);
	    vle.drawHandlers(geom.cloneGeometry(), gs, vp);
	    vea.setSelectionImage(selectionImage);
	} catch (ReadDriverException e) {
	    NotificationManager.addError(e);
	} catch (ValidateRowException e) {
	    NotificationManager.addError(e);
	}

	draw(geometry.cloneGeometry());
    }

    public IGeometry getRemainingGeometry() {

	// TODO [NachoV] I don't like LBD solution for get the remaining
	// geometry. Now it can introduce wrong geometries
	// (with outside points, also following geometries on the same
	// geometry).
	// TODO Better result will be obtain the geometry difference.
	doShortPath = !doShortPath;
	return getCuttedGeometry();

    }

    public IRowEdited getSelectedRow() {
	return selectedRow;
    }

    public ArrayList<IGeometry> getGeometriesCreated() {
	return geomsArray;
    }
}