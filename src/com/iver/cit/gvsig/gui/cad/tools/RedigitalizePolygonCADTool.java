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

import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.iver.andami.PluginServices;
import com.iver.cit.gvsig.CADExtension;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.GeneralPathX;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.ShapeFactory;
import com.iver.cit.gvsig.fmap.core.v02.FConverter;
import com.iver.cit.gvsig.gui.cad.exception.CommandException;
import com.iver.cit.gvsig.gui.cad.tools.smc.RedigitalizePolygonCADToolContext;
import com.iver.cit.gvsig.layers.VectorialLayerEdited;

/**
 * Tool to redigitalize polygons.
 * 
 * @author Jose Ignacio Lamas Fonte [LBD]
 * @author Nacho Varela [Cartolab]
 * @author Pablo Sanxiao [CartoLab]
 * 
 */
public class RedigitalizePolygonCADTool extends CutRedigitalizeCommons {

    private RedigitalizePolygonCADToolContext _fsm;

    @Override
    public void init() {
	// super.init();
	// clear();
	if (_fsm == null) {
	    _fsm = new RedigitalizePolygonCADToolContext(this);
	}
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
	    if (s.equals(PluginServices.getText(this, "removePoint"))) {
		if (newPoints != null) {
		    _fsm.removePoint(null, newPoints.size());
		} else {
		    _fsm.removePoint(null, 0);
		}
	    }
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

	Point2D pointAux = null;
	int sizePixels = 12;
	int half = sizePixels / 2;
	if (firstPoint != null) {
	    pointAux = CADExtension.getEditionManager().getMapControl()
		    .getViewPort().fromMapPoint(firstPoint);
	    drawPixel(pointAux, g, sizePixels, half, firstPoint);

	    // Painting the line where the user must choose the next point
	    if (secondPoint == null) {
		GeneralPathX gpx = new GeneralPathX();
		move_line_asc(0, oldPoints.size() - 1, gpx);
		drawGeometryAndHandlers(g, gpx);
	    }
	}
	if (secondPoint != null) {
	    pointAux = CADExtension.getEditionManager().getMapControl()
		    .getViewPort().fromMapPoint(secondPoint);
	    drawPixel(pointAux, g, sizePixels, half, secondPoint);

	    // Painting the part of the line that we will keep

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
		    line_desc(firstCutIndex, 0, oldPoints, gpx);

		} else {
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
		    line_asc(0, oldPoints.size(), oldPoints, gpx);

		} else {

		    move_line_desc(secondCutIndex, firstCutIndex, gpx);
		    pointsDigitizedByTheUser(gpx);
		    snapperPoints(g, pointsList, gpx, 10);

		    gpx.closePath();
		}
	    }

	    // Checking if the points are well ordered
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
	return PluginServices.getText(this, "redigitalize_polygon_");
    }

    @Override
    public String toString() {
	return "_redigitalizar_linea";
    }

    @Override
    public boolean isApplicable(int shapeType) {
	switch (shapeType) {
	// [LBD comment]
	// case GeometryTypes.POLYGON:
	// case GeometryTypes.MULTIPOLYGON:
	case FShape.POLYGON:
	    return true;
	}
	return false;
    }

    @Override
    public void drawOperation(Graphics g, double x, double y) {
	ArrayList lista = new ArrayList();
	lista.add(new Point2D.Double(x, y));
	drawOperation(g, lista);

    }

    /**
     * It saves the changes that were made to the geometry.
     */
    public void saveChanges() {
	if (selectedRow != null) {
	    System.out.println("--->>> Saving changes in the geometry");
	    ((IFeature) selectedRow.getLinkedRow())
		    .setGeometry(getCuttedGeometry());
	    modifyFeature(selectedRow.getIndex(),
		    (IFeature) selectedRow.getLinkedRow());
	    // updateGeometry(entidadSeleccionada.getIndex());
	}
    }

    @Override
    public IGeometry getCuttedGeometry() {
	final int specialFirstPointIdx = firstPointIndex;

	GeneralPathX gpx = new GeneralPathX();
	PathIterator iterator = selectedGeom.getPathIterator(null,
		FConverter.FLATNESS);
	double[] theData = new double[6];
	int numberMultiActual = 0;

	while (!iterator.isDone()) {
	    int theType = iterator.currentSegment(theData);

	    switch (theType) {
	    case PathIterator.SEG_MOVETO:
		numberMultiActual++;

		if (multiSelected == numberMultiActual) {
		    // Storing here the geometry redigitalized

		    boolean reversePath = firstPointIndex > secondPointIndex;
		    if (reversePath) {
			swapFirstWithSecondPoint();

			if (isSomeConditionHappening()) {

			    move_line_desc(oldPoints.size() - 1,
				    secondPointIndex, gpx);
			    pointsDigitizedByTheUser(gpx);
			    line_desc(specialFirstPointIdx, 0, oldPoints, gpx);

			} else {

			    // Covering first the points between second index
			    // and first index and
			    // then the points digitalized by the user
			    move_line_asc(firstPointIndex, secondPointIndex,
				    gpx);

			    pointsDigitizedByTheUser(gpx);
			    gpx.closePath();
			}

		    } else {

			if (isSomeConditionHappening()) {
			    // We have to cover first the points between the
			    // second and the first
			    // index and then the introduced by the user
			    move_line_asc(0, firstPointIndex, gpx);
			    pointsDigitizedByTheUser(gpx);
			    line_asc(secondPointIndex, oldPoints.size(),
				    oldPoints, gpx);

			} else {
			    // Covering first the points between second index
			    // and first index and
			    // then the points digitalized by the user
			    for (int i = secondPointIndex; i >= firstPointIndex; i--) {
				Point2D point = oldPoints.get(i);
				if (i == secondPointIndex) {
				    gpx.moveTo(point.getX(), point.getY());
				} else {
				    gpx.lineTo(point.getX(), point.getY());
				}
			    }

			    pointsDigitizedByTheUser(gpx);
			    gpx.closePath();
			}
		    }
		} else {
		    makeMoveToSegmentWithTheData(gpx, theData);
		}
		break;

	    case PathIterator.SEG_LINETO:
		processSEG_LINETO(theData, theType, gpx);
		break;
	    case PathIterator.SEG_CLOSE:
		processSEG_CLOSE(theType, gpx);
		break;

	    } // end switch
	    iterator.next();
	    System.out.println("Closing polygon");
	}

	// Checking if the points are well ordered
	if (gpx.isCCW()) {
	    gpx.flip();
	}

	IGeometry geom = ShapeFactory.createPolygon2D(gpx);
	return geom;
    }

    @Override
    public void clear() {
	super.clear();
	try {
	    VectorialLayerEdited vle = ((VectorialLayerEdited) CADExtension
		    .getEditionManager().getActiveLayerEdited());
	    if (vle != null) {
		vle.clearSelection(true);
	    }
	} catch (ReadDriverException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	_fsm = new RedigitalizePolygonCADToolContext(this);
    }
}
