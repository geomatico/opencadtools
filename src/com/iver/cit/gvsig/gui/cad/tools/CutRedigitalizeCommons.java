package com.iver.cit.gvsig.gui.cad.tools;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;

import com.iver.cit.gvsig.CADExtension;
import com.iver.cit.gvsig.fmap.core.GeneralPathX;
import com.iver.cit.gvsig.fmap.core.Handler;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.ShapeFactory;
import com.iver.cit.gvsig.fmap.core.v02.FConverter;
import com.iver.cit.gvsig.fmap.core.v02.FGraphicUtilities;
import com.iver.cit.gvsig.fmap.edition.DefaultRowEdited;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.gui.cad.CADTool;
import com.iver.cit.gvsig.gui.cad.DefaultCADTool;
import com.iver.cit.gvsig.layers.VectorialLayerEdited;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

public abstract class CutRedigitalizeCommons extends DefaultCADTool {

    protected IGeometry selectedGeom;
    protected ArrayList<Double> oldPoints;
    protected ArrayList<Double> newPoints;
    protected IRowEdited selectedRow;
    protected Point2D firstPoint;
    protected Point2D secondPoint;
    protected int firstPointIndex = -1;
    protected int secondPointIndex = -1;
    protected double PROXIMITY_THRESHOLD = 0.00001; // En cut polygon tenía un
						    // cero más
    protected int multiSelected;
    protected boolean secondPointContentVertex = true;
    protected boolean doShortPath = true;

    @Override
    public void clear() {
	selectedGeom = null;
	firstPoint = null;
	secondPoint = null;
	oldPoints = null;
	newPoints = null;
	firstPointIndex = -1;
	secondPointIndex = -1;
	secondPointContentVertex = true;
	this.setMultiTransition(false);
	getCadToolAdapter().setPreviousPoint((double[]) null);
	selectedRow = null;
	doShortPath = true;
	multiSelected = 0;
    }

    /**
     * It detects if the point is inside the outline of the selected geometry at
     * this time and store the information related to the situation and the rest
     * of te points of the geometry.
     */
    public boolean pointInsideFeature(double x, double y) {
	boolean isInside = false;

	VectorialLayerEdited vle = (VectorialLayerEdited) CADExtension
		.getEditionManager().getActiveLayerEdited();

	vle.selectWithPoint(x, y, false);
	ArrayList selectedRows = getSelectedRows();

	if (selectedRows.size() > 0) {
	    selectedRow = (DefaultRowEdited) selectedRows.get(0);
	    selectedGeom = ((IFeature) selectedRow.getLinkedRow())
		    .getGeometry();
	    firstPoint = new Point2D.Double(x, y);

	    // filling points list
	    oldPoints = new ArrayList<Double>();
	    Coordinate c = new Coordinate(x, y);

	    PathIterator theIterator = selectedGeom.getPathIterator(null,
		    FConverter.FLATNESS);
	    boolean terminated = false;
	    double[] theData = new double[6];
	    Coordinate from = null;
	    Coordinate first = null;
	    int numberMultiActual = 0;
	    int index = 0;

	    while (!theIterator.isDone() && !terminated) {
		int theType = theIterator.currentSegment(theData);

		switch (theType) {
		case PathIterator.SEG_MOVETO:
		    from = new Coordinate(theData[0], theData[1]);
		    first = from;

		    numberMultiActual++;
		    // We initialize each time in order to store only the
		    // geometry of the multi geometry which contains the point
		    if (multiSelected != 0) {
			terminated = true;
		    } else {
			oldPoints = new ArrayList<Double>();
			index = 0;
			oldPoints.add(index, new Point2D.Double(theData[0],
				theData[1]));
		    }
		    if (c.equals(from)) {
			firstPointIndex = index;
			multiSelected = numberMultiActual;
			isInside = true;
		    }
		    break;

		case PathIterator.SEG_LINETO:

		    Coordinate to = new Coordinate(theData[0], theData[1]);
		    LineSegment line = new LineSegment(from, to);
		    Coordinate closestPoint = line.closestPoint(c);
		    double dist = c.distance(closestPoint);
		    if (c.equals(to)) {
			firstPointIndex = index;
			multiSelected = numberMultiActual;
			isInside = true;
		    } else if ((dist < PROXIMITY_THRESHOLD)
			    && (!c.equals(from))) {
			firstPointIndex = index;
			oldPoints.add(index, new Point2D.Double(x, y));
			index++;
			multiSelected = numberMultiActual;
			isInside = true;
		    }
		    oldPoints.add(index, new Point2D.Double(theData[0],
			    theData[1]));
		    from = to;
		    break;
		case PathIterator.SEG_CLOSE:
		    line = new LineSegment(from, first);

		    closestPoint = line.closestPoint(c);
		    dist = c.distance(closestPoint);
		    if (c.equals(first)) {
			firstPointIndex = index;
			multiSelected = numberMultiActual;
			isInside = true;
		    } else if ((dist < PROXIMITY_THRESHOLD)
			    && (!c.equals(from))) {
			firstPointIndex = index;
			oldPoints.add(index, new Point2D.Double(x, y));
			index++;
			multiSelected = numberMultiActual;
			isInside = true;
		    }
		    oldPoints.add(index, new Point2D.Double(first.x, first.y));
		    from = first;
		    break;

		}
		index++;
		theIterator.next();
	    }
	    if (!isInside) {
		// In this case the point pressed will be inside the polygon but
		// it wont be inside the line that contains the polygon so we
		// must reset the geometry
		clear();
	    }
	}
	getCadToolAdapter().setPreviousPoint((double[]) null);
	return isInside;
    }

    public void snapperPoints(Graphics g, ArrayList<Point2D> pointsList,
	    GeneralPathX gpx, int sizePixelsSnapper) {
	if (pointsList != null) {
	    for (int i = 0; i < pointsList.size(); i++) {
		Point2D point = pointsList.get(i);
		gpx.lineTo(point.getX(), point.getY());
		if (i < pointsList.size() - 1) {
		    Point2D actual = null;
		    actual = CADExtension.getEditionManager().getMapControl()
			    .getViewPort().fromMapPoint(point);
		    int halfSnapper = sizePixelsSnapper / 2;
		    g.drawRect((int) (actual.getX() - halfSnapper),
			    (int) (actual.getY() - halfSnapper),
			    sizePixelsSnapper, sizePixelsSnapper);
		}
	    }
	}
    }

    public void drawPixel(Point2D pointAux, Graphics g, int sizePixels,
	    int half, Point2D firstPoint) {
	// TODO: fpuga: Probably this name should be changed
	g.drawRect((int) (pointAux.getX() - (half - 2)),
		(int) (pointAux.getY() - (half - 2)), sizePixels - 4,
		sizePixels - 4);
	g.drawRect((int) (pointAux.getX() - half),
		(int) (pointAux.getY() - half), sizePixels, sizePixels);
    }

    public void drawGeometryAndHandlers(Graphics g, GeneralPathX gpx) {
	IGeometry geom = ShapeFactory.createPolygon2D(gpx);
	geom.draw((Graphics2D) g, CADExtension.getEditionManager()
		.getMapControl().getViewPort(), CADTool.drawingSymbol);

	// Painting vertex
	AffineTransform at = CADExtension.getEditionManager().getMapControl()
		.getViewPort().getAffineTransform();
	Handler[] h = geom.getHandlers(IGeometry.SELECTHANDLER);
	FGraphicUtilities.DrawHandlers((Graphics2D) g, at, h,
		CADTool.drawingSymbol);
    }

    public IGeometry getCuttedGeometry() {
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
		    }

		    if (isSomeConditionHappening()) {

			if (reversePath) {
			    move_line_desc(oldPoints.size() - 2,
				    secondPointIndex, gpx);
			} else {
			    // We have to cover first the points between the
			    // second and the first
			    // index and then the introduced by the user
			    move_line_asc(0, firstPointIndex, gpx);
			}

			pointsDigitizedByTheUser(gpx);

			// The rest of the points of the list
			if (reversePath) {
			    line_desc(firstPointIndex, 0, oldPoints, gpx);
			} else {
			    line_asc(secondPointIndex, oldPoints.size() - 1,
				    oldPoints, gpx);
			}
			gpx.closePath();

		    } else {
			move_line_asc(firstPointIndex, secondPointIndex, gpx);

			// Point digitalized by the user (the order depends on
			// reversePath)
			if (newPoints != null && newPoints.size() > 0) {
			    if (reversePath) {
				line_asc(0, newPoints.size(), newPoints, gpx);
			    } else {
				line_desc(newPoints.size() - 1, 0, newPoints,
					gpx);
			    }
			}
			gpx.closePath();
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

	    }
	    iterator.next();
	}

	// Checking if the points are well ordered
	// fpuga: This lines should be carefully checked if the nodes seems
	// unordered
	// if(gpx.isCCW()){
	// gpx.flip();
	// }

	IGeometry geom = ShapeFactory.createPolygon2D(gpx);
	return geom;
    }

    protected void line_desc(int startPoint, int endPoint,
	    ArrayList<Double> pointList, GeneralPathX gpx) {
	for (int i = startPoint; i >= endPoint; i--) {
	    Point2D point = pointList.get(i);
	    System.out.println("lineTo: " + point.getX() + ", " + point.getY());
	    gpx.lineTo(point.getX(), point.getY());
	}
    }

    protected void line_asc(int startPoint, int endPoint,
	    ArrayList<Double> pointList, GeneralPathX gpx) {
	for (int i = startPoint; i < endPoint; i++) {
	    Point2D point = pointList.get(i);
	    gpx.lineTo(point.getX(), point.getY());
	}
    }

    protected void move_line_asc(int startPoint, int endPoint, GeneralPathX gpx) {

	for (int i = startPoint; i <= endPoint; i++) {
	    Point2D point = oldPoints.get(i);
	    if (i == startPoint) {
		System.out.println("moveTo: " + point.getX() + ", "
			+ point.getY());
		gpx.moveTo(point.getX(), point.getY());
	    } else {
		System.out.println("lineTo: " + point.getX() + ", "
			+ point.getY());
		gpx.lineTo(point.getX(), point.getY());
	    }
	}
    }

    protected void move_line_desc(int initPoint, int endPoint, GeneralPathX gpx) {
	for (int i = initPoint; i >= endPoint; i--) {
	    Point2D point = oldPoints.get(i);
	    if (i == initPoint) {
		gpx.moveTo(point.getX(), point.getY());
	    } else {
		gpx.lineTo(point.getX(), point.getY());
	    }
	}
    }

    protected void pointsDigitizedByTheUser(GeneralPathX gpx) {
	// Points digitalized by the user
	if (newPoints != null && newPoints.size() > 0) {
	    line_asc(0, newPoints.size(), newPoints, gpx);
	}
    }

    protected void swapFirstWithSecondPoint() {
	System.out.println("Primer punto > segundo Punto");
	int aux = firstPointIndex;
	firstPointIndex = secondPointIndex;
	secondPointIndex = aux;
    }

    protected void makeMoveToSegmentWithTheData(GeneralPathX gpx,
	    double[] theData) {
	double x = theData[0];
	double y = theData[1];
	System.out.println("moveTo: " + x + ", " + y);
	gpx.moveTo(x, y);
    }

    protected boolean isSomeConditionHappening() {
	return ((((secondPointIndex - firstPointIndex) * 2) <= oldPoints.size()) && (doShortPath))
		|| ((((secondPointIndex - firstPointIndex) * 2) > oldPoints
			.size()) && (!doShortPath));
    }

    protected void processSEG_LINETO(double[] theData, int numberMultiActual,
	    GeneralPathX gpx) {
	double x = theData[0];
	double y = theData[1];
	if (multiSelected == numberMultiActual) {
	    // Nothing to do
	} else {
	    System.out.println("lineTo: " + x + ", " + y);
	    gpx.lineTo(x, y);
	}
    }

    protected void processSEG_CLOSE(int numberMultiActual, GeneralPathX gpx) {

	if (multiSelected == numberMultiActual) {
	    // Nothing to do
	} else {
	    System.out.println("Closing polygon");
	    gpx.closePath();
	}
    }

    public void removePoint(InputEvent event) {
	newPoints.remove(newPoints.size() - 1);
	if (newPoints.size() > 0) {
	    getCadToolAdapter().setPreviousPoint(
		    newPoints.get(newPoints.size() - 1));
	} else {
	    getCadToolAdapter().setPreviousPoint((double[]) null);
	}
    }

    public void removeFirstPoint(InputEvent event) {
	clear();
    }

    /**
     * Deleting the memory of the snappers in order to click in two points of
     * the same geometry and they don't follow the ones that there are between
     * them.
     */
    public void cleanSnapper() {

	if (newPoints == null || newPoints.size() == 0) {
	    getCadToolAdapter().setPreviousPoint((double[]) null);
	}

    }

    /**
     * Alternate the part of the geometry that we keep when the cut is made
     */
    public void changePieceOfGeometry() {
	doShortPath = !doShortPath;
	CADExtension.getEditionManager().getMapControl().repaint();
    }

    public void setPreviousTool(DefaultCADTool tool) {
    }

    @Override
    public boolean isMultiTransition() {
	return true;
    }

    @Override
    public void setMultiTransition(boolean condicion) {
    }

    /**
     * It detects if the point is inside the outline of the selected geometry at
     * this time and store the information related to the situation and the rest
     * of the points of the geometry. This method must be executed when the
     * first intersection point was established
     */
    public boolean secondPointInsideFeature(double x, double y) {
	boolean isInside = false;

	VectorialLayerEdited vle = (VectorialLayerEdited) CADExtension
		.getEditionManager().getActiveLayerEdited();

	vle.selectWithPoint(x, y, false);
	ArrayList selectedRows = getSelectedRows();

	if (selectedRows.size() > 0) {

	    Coordinate c = new Coordinate(x, y);

	    Coordinate from = null;
	    boolean found = false;
	    int index = 0;
	    boolean terminatePoints = (oldPoints == null || oldPoints.size() == 0);
	    if (!terminatePoints) {
		Point2D point = oldPoints.get(0);
		from = new Coordinate(point.getX(), point.getY());
	    }
	    while (!terminatePoints && !found) {
		Point2D point = oldPoints.get(index);
		Coordinate to = new Coordinate(point.getX(), point.getY());
		LineSegment line = new LineSegment(from, to);
		Coordinate closestPoint = line.closestPoint(c);
		double dist = c.distance(closestPoint);
		if (c.equals(to)) {
		    secondPointContentVertex = true;
		    secondPointIndex = index;
		    isInside = true;
		    found = true;
		} else if ((dist < PROXIMITY_THRESHOLD) && (!c.equals(from))) {
		    secondPointContentVertex = false;
		    secondPointIndex = index;
		    oldPoints.add(index, new Point2D.Double(x, y));
		    index++;
		    isInside = true;
		    found = true;
		}
		from = to;

		index++;
		if (index == oldPoints.size()) {
		    terminatePoints = true;
		}
	    }
	}

	if (isInside) {
	    secondPoint = new Point2D.Double(x, y);
	    if ((firstPointIndex >= secondPointIndex)
		    && (!secondPointContentVertex)) {
		firstPointIndex++;
	    }
	    newPoints = new ArrayList<Double>();
	    this.setMultiTransition(true);
	}
	getCadToolAdapter().setPreviousPoint((double[]) null);
	return isInside;
    }

    public void removeSecondPoint(InputEvent event) {
	// Checking if the point was on the vertex and if so eliminate it of the
	// points list
	if (!secondPointContentVertex) {
	    oldPoints.remove(secondPointIndex);
	}
	secondPointIndex = -1;
	secondPointContentVertex = true;
	secondPoint = null;
	this.setMultiTransition(false);
	getCadToolAdapter().setPreviousPoint((double[]) null);

    }

}