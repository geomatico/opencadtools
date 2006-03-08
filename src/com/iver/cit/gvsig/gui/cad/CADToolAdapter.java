package com.iver.cit.gvsig.gui.cad;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.util.List;
import java.util.Stack;

import com.iver.andami.PluginServices;
import com.iver.cit.gvsig.CADExtension;
import com.iver.cit.gvsig.fmap.DriverException;
import com.iver.cit.gvsig.fmap.ViewPort;
import com.iver.cit.gvsig.fmap.core.Handler;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.v02.FConstant;
import com.iver.cit.gvsig.fmap.core.v02.FConverter;
import com.iver.cit.gvsig.fmap.core.v02.FSymbol;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.VectorialEditableAdapter;
import com.iver.cit.gvsig.fmap.layers.FBitSet;
import com.iver.cit.gvsig.fmap.tools.BehaviorException;
import com.iver.cit.gvsig.fmap.tools.Behavior.Behavior;
import com.iver.cit.gvsig.fmap.tools.Listeners.ToolListener;
import com.iver.cit.gvsig.gui.View;
import com.iver.cit.gvsig.gui.cad.tools.SelectionCADTool;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class CADToolAdapter extends Behavior {
	public static int MAX_ENTITIES_IN_SPATIAL_CACHE = 5000; 
	
	private Stack cadToolStack = new Stack();

	// Para pasarle las coordenadas cuando se produce un evento textEntered
	private int lastX;

	private int lastY;

	private FSymbol symbol = new FSymbol(FConstant.SYMBOL_TYPE_POINT, Color.RED);

	private Point2D mapAdjustedPoint;

	private boolean questionAsked = false;

	private Point2D adjustedPoint;

	private boolean snapping = false;

	private boolean adjustSnapping = false;

	private VectorialEditableAdapter vea;

	private CADGrid cadgrid = new CADGrid();

	private SpatialIndex spatialCache;

	/**
	 * Pinta de alguna manera especial las geometrias seleccionadas para la
	 * edici�n. En caso de que el snapping est� activado, pintar� el efecto del
	 * mismo.
	 *
	 * @see com.iver.cit.gvsig.fmap.tools.Behavior.Behavior#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		drawCursor(g);
		getGrid().drawGrid(g);
		if (adjustedPoint != null) {
			Point2D p = null;
			if (mapAdjustedPoint != null) {
				p = mapAdjustedPoint;
			} else {
				p = getMapControl().getViewPort().toMapPoint(adjustedPoint);
			}
			((CADTool) cadToolStack.peek())
					.drawOperation(g, p.getX(), p.getY());
		}
	}

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) throws BehaviorException {
		if (e.getButton() == MouseEvent.BUTTON3) {
			CADExtension.showPopup(e);
		}
	}

	/**
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) throws BehaviorException {
		clearMouseImage();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) throws BehaviorException {
	}

	/**
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) throws BehaviorException {
		if (e.getButton() == MouseEvent.BUTTON1) {
			ViewPort vp = getMapControl().getMapContext().getViewPort();
			Point2D p;

			if (mapAdjustedPoint != null) {
				p = mapAdjustedPoint;
			} else {
				p = vp.toMapPoint(adjustedPoint);
			}
			transition(new double[] { p.getX(), p.getY() }, e);
		}
	}

	/**
	 * Ajusta un punto de la imagen que se pasa como par�metro al grid si �ste
	 * est� activo y devuelve la distancia de un punto al punto ajustado
	 *
	 * @param point
	 * @param mapHandlerAdjustedPoint
	 *            DOCUMENT ME!
	 *
	 * @return Distancia del punto que se pasa como par�metro al punto ajustado
	 */
	private double adjustToHandler(Point2D point,
			Point2D mapHandlerAdjustedPoint) {
		// if (selection.cardinality() > 0) {
		if (getSpatialCache() == null)
			return Double.MAX_VALUE; 
		
		double rw = getMapControl().getViewPort().toMapDistance(5);
		Point2D mapPoint = point;
		Rectangle2D r = new Rectangle2D.Double(mapPoint.getX() - rw / 2,
				mapPoint.getY() - rw / 2, rw, rw);

		// int[] indexes = vea.getRowsIndexes_OLD(r);
		Envelope e = FConverter.convertRectangle2DtoEnvelope(r);
		List l = getSpatialCache().query(e);
		double min = Double.MAX_VALUE;
		Point2D argmin = null;
		Point2D mapArgmin = null;

		for (int i = 0; i < l.size(); i++) {
		// for (int i = 0; i < indexes.length; i++) {
			IGeometry geometry = null;
			/* try {
				geometry = vea.getShape(indexes[i]);
			} catch (DriverIOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} */
			geometry = (IGeometry) l.get(i);// getFeature(indexes[i]);
			Handler[] handlers = geometry.getHandlers(IGeometry.SELECTHANDLER);

			for (int j = 0; j < handlers.length; j++) {
				Point2D handlerPoint = handlers[j].getPoint();
				// System.err.println("handlerPoint= "+ handlerPoint);
				Point2D handlerImagePoint = handlerPoint;
				double dist = handlerImagePoint.distance(point);
				if ((dist < getMapControl().getViewPort().toMapDistance(
						SelectionCADTool.tolerance))
						&& (dist < min)) {
					min = dist;
					argmin = handlerImagePoint;
					mapArgmin = handlerPoint;
				}
			}
		}

		if (argmin != null) {
			point.setLocation(argmin);

			// Se hace el casting porque no se quiere redondeo
			point.setLocation(argmin.getX(), argmin.getY());

			mapHandlerAdjustedPoint.setLocation(mapArgmin);

			return min;
		}

		return Double.MAX_VALUE;

	}

	/**
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) throws BehaviorException {
		getMapControl().repaint();
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) throws BehaviorException {
		lastX = e.getX();
		lastY = e.getY();

		calculateSnapPoint(e.getPoint());
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) throws BehaviorException {

		lastX = e.getX();
		lastY = e.getY();

		calculateSnapPoint(e.getPoint());

		getMapControl().repaint();
	}

	private void clearMouseImage() {
		int[] pixels = new int[16 * 16];
		Image image = Toolkit.getDefaultToolkit().createImage(
				new MemoryImageSource(16, 16, pixels, 0, 16));
		Cursor transparentCursor = Toolkit.getDefaultToolkit()
				.createCustomCursor(image, new Point(0, 0), "invisiblecursor");

		getMapControl().setCursor(transparentCursor);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param g
	 *            DOCUMENT ME!
	 */
	private void drawCursor(Graphics g) {

		Point2D p = adjustedPoint;

		if (p == null) {
			getGrid().setViewPort(getMapControl().getViewPort());

			return;
		}

		int size1 = 15;
		int size2 = 3;
		g.drawLine((int) (p.getX() - size1), (int) (p.getY()),
				(int) (p.getX() + size1), (int) (p.getY()));
		g.drawLine((int) (p.getX()), (int) (p.getY() - size1),
				(int) (p.getX()), (int) (p.getY() + size1));

		if (adjustedPoint != null) {
			if (adjustSnapping) {
				g.setColor(Color.ORANGE);
				g.drawRect((int) (adjustedPoint.getX() - 6),
						(int) (adjustedPoint.getY() - 6), 12, 12);
				g.drawRect((int) (adjustedPoint.getX() - 3),
						(int) (adjustedPoint.getY() - 3), 6, 6);
				g.setColor(Color.MAGENTA);
				g.drawRect((int) (adjustedPoint.getX() - 4),
						(int) (adjustedPoint.getY() - 4), 8, 8);

				adjustSnapping = false;
			} else {
				g.drawRect((int) (p.getX() - size2), (int) (p.getY() - size2),
						(int) (size2 * 2), (int) (size2 * 2));
			}
		}
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param point
	 */
	private void calculateSnapPoint(Point point) {
		// Se comprueba el ajuste a rejilla

		Point2D gridAdjustedPoint = getMapControl().getViewPort().toMapPoint(
				point);
		double minDistance = Double.MAX_VALUE;
		CADTool ct = (CADTool) cadToolStack.peek();
		if (ct instanceof SelectionCADTool
				&& ((SelectionCADTool) ct).getStatus().equals(
						"ExecuteMap.Initial")) {
			mapAdjustedPoint = gridAdjustedPoint;
			adjustedPoint = (Point2D) point.clone();
		} else {

			minDistance = getGrid().adjustToGrid(gridAdjustedPoint);
			if (minDistance < Double.MAX_VALUE) {
				adjustedPoint = getMapControl().getViewPort().fromMapPoint(
						gridAdjustedPoint);
				mapAdjustedPoint = gridAdjustedPoint;
			} else {
				mapAdjustedPoint = null;
			}
		}
		Point2D handlerAdjustedPoint = null;

		// Se comprueba el ajuste a los handlers
		if (mapAdjustedPoint != null) {
			handlerAdjustedPoint = (Point2D) mapAdjustedPoint.clone(); // getMapControl().getViewPort().toMapPoint(point);
		} else {
			handlerAdjustedPoint = getMapControl().getViewPort().toMapPoint(
					point);
		}

		Point2D mapPoint = new Point2D.Double();
		double distance = adjustToHandler(handlerAdjustedPoint, mapPoint);

		if (distance < minDistance) {
			adjustSnapping = true;
			adjustedPoint = getMapControl().getViewPort().fromMapPoint(
					handlerAdjustedPoint);
			mapAdjustedPoint = mapPoint;
			minDistance = distance;
		}

		// Si no hay ajuste
		if (minDistance == Double.MAX_VALUE) {
			adjustedPoint = point;
			mapAdjustedPoint = null;
		}

	}

	/**
	 * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
	 */
	public void mouseWheelMoved(MouseWheelEvent e) throws BehaviorException {
		getMapControl().cancelDrawing();
		ViewPort vp = getMapControl().getViewPort();
		// Point2D pReal = vp.toMapPoint(e.getPoint());

		Point2D pReal = new Point2D.Double(vp.getAdjustedExtent().getCenterX(),
				vp.getAdjustedExtent().getCenterY());
		int amount = e.getWheelRotation();
		double nuevoX;
		double nuevoY;
		double factor;

		if (amount > 0) // nos acercamos
		{
			factor = 0.9;
		} else // nos alejamos
		{
			factor = 1.2;
		}
		Rectangle2D.Double r = new Rectangle2D.Double();
		if (vp.getExtent() != null) {
			nuevoX = pReal.getX()
					- ((vp.getExtent().getWidth() * factor) / 2.0);
			nuevoY = pReal.getY()
					- ((vp.getExtent().getHeight() * factor) / 2.0);
			r.x = nuevoX;
			r.y = nuevoY;
			r.width = vp.getExtent().getWidth() * factor;
			r.height = vp.getExtent().getHeight() * factor;

			vp.setExtent(r);
		}
	}

	/**
	 * M�todo que realiza las transiciones en las herramientas en funci�n de un
	 * texto introducido en la consola
	 *
	 * @param text
	 *            DOCUMENT ME!
	 */
	public void textEntered(String text) {
		if (text == null) {
			transition("cancel");
		} else {
			/*
			 * if ("".equals(text)) { transition("aceptar"); } else {
			 */
			text = text.trim();

			String[] numbers = text.split(",");
			double[] values = null;

			try {
				if (numbers.length == 2) {
					// punto
					values = new double[] { Double.parseDouble(numbers[0]),
							Double.parseDouble(numbers[1]) };
					transition(values, null);
				} else if (numbers.length == 1) {
					// valor
					values = new double[] { Double.parseDouble(numbers[0]) };
					transition(values[0]);
				}
			} catch (NumberFormatException e) {
				transition(text);
			}
			// }
		}
		getMapControl().repaint();
	}


	/**
	 * DOCUMENT ME!
	 */
	public void configureMenu() {
		String[] desc = ((CADTool) cadToolStack.peek()).getDescriptions();
		// String[] labels = ((CADTool)
		// cadToolStack.peek()).getCurrentTransitions();
		CADExtension.clearMenu();

		for (int i = 0; i < desc.length; i++) {
			if (desc[i] != null) {
				CADExtension.addMenuEntry(desc[i]);// , labels[i]);
			}
		}

	}

	/**
	 * Recibe los valores de la transici�n (normalmente un punto) y el evento
	 * con el que se gener� (si fue de rat�n ser� MouseEvent, el que viene
	 * en el pressed) y si es de teclado, ser� un KeyEvent.
	 * Del evento se puede sacar informaci�n acerca de si estaba pulsada la tecla
	 * CTRL, o Alt, etc.
	 * @param values
	 * @param event
	 */
	private void transition(double[] values, InputEvent event) {
		questionAsked = true;
		if (!cadToolStack.isEmpty()) {
			CADTool ct = (CADTool) cadToolStack.peek();
			// /String[] trs = ct.getAutomaton().getCurrentTransitions();
			boolean esta = true;
			/*
			 * for (int i = 0; i < trs.length; i++) { if
			 * (trs[i].toUpperCase().equals(text.toUpperCase())) esta = true; }
			 */
			if (!esta) {
				askQuestion();
			} else {
				ct.transition(values[0], values[1], event);
				// Si es la transici�n que finaliza una geometria hay que
				// redibujar la vista.

				askQuestion();
				/*
				 * if ((ret & Automaton.AUTOMATON_FINISHED) ==
				 * Automaton.AUTOMATON_FINISHED) { popCadTool();
				 *
				 * if (cadToolStack.isEmpty()) { pushCadTool(new
				 * com.iver.cit.gvsig.gui.cad.smc.gen.CADTool());//new
				 * SelectionCadTool());
				 * PluginServices.getMainFrame().setSelectedTool("selection"); }
				 *
				 * askQuestion();
				 *
				 * getMapControl().drawMap(false); } else { if (((CadTool)
				 * cadToolStack.peek()).getAutomaton().checkState('c')) {
				 * getMapControl().drawMap(false); }
				 *
				 * if (!questionAsked) { askQuestion(); } }
				 *
				 * configureMenu();
				 */
			}
		}
		configureMenu();
		PluginServices.getMainFrame().enableControls();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param text
	 *            DOCUMENT ME!
	 * @param source
	 *            DOCUMENT ME!
	 * @param sel
	 *            DOCUMENT ME!
	 * @param values
	 *            DOCUMENT ME!
	 */
	private void transition(double value) {
		questionAsked = true;
		if (!cadToolStack.isEmpty()) {
			CADTool ct = (CADTool) cadToolStack.peek();
			ct.transition(value);
			askQuestion();
		}
		configureMenu();
		PluginServices.getMainFrame().enableControls();
	}

	public void transition(String option) {
		questionAsked = true;
		if (!cadToolStack.isEmpty()) {
			CADTool ct = (CADTool) cadToolStack.peek();
			ct.transition(option);
			askQuestion();
		}
		configureMenu();
		PluginServices.getMainFrame().enableControls();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param value
	 *            DOCUMENT ME!
	 */
	public void setGrid(boolean value) {
		getGrid().setUseGrid(value);
		getGrid().setViewPort(getMapControl().getViewPort());
		getMapControl().drawMap(false);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param activated
	 *            DOCUMENT ME!
	 */
	public void setSnapping(boolean activated) {
		snapping = activated;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param x
	 *            DOCUMENT ME!
	 * @param y
	 *            DOCUMENT ME!
	 * @param dist
	 *            DOCUMENT ME!
	 */
	public void getSnapPoint(double x, double y, double dist) {
	}

	/**
	 * @see com.iver.cit.gvsig.fmap.tools.Behavior.Behavior#getListener()
	 */
	public ToolListener getListener() {
		return new ToolListener() {
			/**
			 * @see com.iver.cit.gvsig.fmap.tools.Listeners.ToolListener#getCursor()
			 */
			public Cursor getCursor() {
				return null;
			}

			/**
			 * @see com.iver.cit.gvsig.fmap.tools.Listeners.ToolListener#cancelDrawing()
			 */
			public boolean cancelDrawing() {
				return false;
			}
		};
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public CADTool getCadTool() {
		return (CADTool) cadToolStack.peek();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param cadTool
	 *            DOCUMENT ME!
	 */
	public void pushCadTool(CADTool cadTool) {
		cadToolStack.push(cadTool);
		cadTool.setCadToolAdapter(this);
		// cadTool.initializeStatus();
		// cadTool.setVectorialAdapter(vea);
		/*
		 * int ret = cadTool.transition(null, editableFeatureSource, selection,
		 * new double[0]);
		 *
		 * if ((ret & Automaton.AUTOMATON_FINISHED) ==
		 * Automaton.AUTOMATON_FINISHED) { popCadTool();
		 *
		 * if (cadToolStack.isEmpty()) { pushCadTool(new
		 * com.iver.cit.gvsig.gui.cad.smc.gen.CADTool());//new
		 * SelectionCadTool());
		 * PluginServices.getMainFrame().setSelectedTool("selection"); }
		 *
		 * askQuestion();
		 *
		 * getMapControl().drawMap(false); }
		 */
	}

	/**
	 * DOCUMENT ME!
	 */
	public void popCadTool() {
		cadToolStack.pop();
	}

	/**
	 * DOCUMENT ME!
	 */
	public void askQuestion() {
		CADTool cadtool = (CADTool) cadToolStack.peek();
		/*
		 * if (cadtool..getStatus()==0){
		 * PluginServices.getMainFrame().addTextToConsole("\n"
		 * +cadtool.getName()); }
		 */
		View vista = (View) PluginServices.getMDIManager().getActiveView();
		vista.getConsolePanel().addText("\n" + cadtool.getQuestion() + ">");
		// ***PluginServices.getMainFrame().addTextToConsole("\n" +
		// cadtool.getQuestion());
		questionAsked = true;

	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param cadTool
	 *            DOCUMENT ME!
	 */
	public void setCadTool(CADTool cadTool) {
		cadToolStack.clear();
		pushCadTool(cadTool);
		askQuestion();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public VectorialEditableAdapter getVectorialAdapter() {
		return vea;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param editableFeatureSource
	 *            DOCUMENT ME!
	 * @param selection
	 *            DOCUMENT ME!
	 */
	public void setVectorialAdapter(VectorialEditableAdapter vea) {
		this.vea = vea;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	/*
	 * public CadMapControl getCadMapControl() { return cadMapControl; }
	 */
	/**
	 * DOCUMENT ME!
	 *
	 * @param cadMapControl
	 *            DOCUMENT ME!
	 */
	/*
	 * public void setCadMapControl(CadMapControl cadMapControl) {
	 * this.cadMapControl = cadMapControl; }
	 */

	/**
	 * Elimina las geometr�as seleccionadas actualmente
	 */
	private void delete() {
		vea.startComplexRow();
		FBitSet selection = getVectorialAdapter().getSelection();
		try {
			int[] indexesToDel = new int[selection.cardinality()];
			int j = 0;
			for (int i = selection.nextSetBit(0); i >= 0; i = selection
					.nextSetBit(i + 1)) {
				indexesToDel[j++] = i;
				// /vea.removeRow(i);
			}
			for (j = indexesToDel.length - 1; j >= 0; j--) {
				vea.removeRow(indexesToDel[j],"Feature eliminada");
			}
		} catch (DriverIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				vea.endComplexRow();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (DriverIOException e1) {
				e1.printStackTrace();
			}
		}
		System.out.println("clear Selection");
		selection.clear();
		if (getCadTool() instanceof SelectionCADTool)
		{
			SelectionCADTool selTool = (SelectionCADTool) getCadTool();
			selTool.clearSelection();
		}
		getMapControl().drawMap(false);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param b
	 */
	public void setAdjustGrid(boolean b) {
		getGrid().setAdjustGrid(b);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param actionCommand
	 */
	public void keyPressed(String actionCommand) {
		if (actionCommand.equals("eliminar")) {
			delete();
		} else if (actionCommand.equals("escape")) {
			if (getMapControl().getTool().equals("cadtooladapter")) {
				CADTool ct = (CADTool) cadToolStack.peek();
				ct.end();
				cadToolStack.clear();
				SelectionCADTool selCad = new SelectionCADTool();
				selCad.init();
				selCad.clearSelection();
				
				pushCadTool(selCad);
				// getVectorialAdapter().getSelection().clear();
				getMapControl().drawMap(false);
				PluginServices.getMainFrame().setSelectedTool("SELCAD");
				askQuestion();
			}
		}

		PluginServices.getMainFrame().enableControls();

	}

	public CADGrid getGrid() {
		return cadgrid;
	}

	/**
	 * @return Returns the spatialCache.
	 */
	public SpatialIndex getSpatialCache() {
		return spatialCache;
	}

	/**
	 * Se usa para rellenar la cache de entidades
	 * con la que queremos trabajar (para hacer snapping,
	 * por ejemplo. Lo normal ser�
	 * rellenarla cada vez que cambie el extent, y 
	 * bas�ndonos en el futuro EditionManager para saber
	 * de cu�ntos temas hay que leer. Si el numero de entidades
	 * supera MAX_ENTITIES_IN_SPATIAL_CACHE, lo pondremos
	 * a nulo.
	 * @throws DriverException 
	 */
	public void createSpatialCache() throws DriverException {
		ViewPort vp = getMapControl().getViewPort();
		Rectangle2D extent = vp.getAdjustedExtent();
		// TODO: Por ahora cogemos el VectorialAdapter
		// de aqu�, pero deber�amos tener un m�todo que
		// le pregunte al EditionManager el tema sobre
		// el que estamos pintando.
		String strEPSG = vp.getProjection().getAbrev().substring(5);
		IRowEdited[] feats = getVectorialAdapter().getFeatures(extent, strEPSG);
		if (feats.length > MAX_ENTITIES_IN_SPATIAL_CACHE)
			this.spatialCache = null;
		else
			this.spatialCache = new Quadtree();
		for (int i=0; i < feats.length; i++)
		{
			IFeature feat =  (IFeature)feats[i].getLinkedRow();
			IGeometry geom = feat.getGeometry();
			// TODO: EL getBounds2D del IGeometry ralentiza innecesariamente
			// Podr�amos hacer que GeneralPathX lo tenga guardado, 
			// y tenga un constructor en el que se lo fijes.
			// De esta forma, el driver, a la vez que recupera
			// las geometr�as podr�a calcular el boundingbox
			// y asignarlo. Luego habr�a que poner un m�todo
			// que recalcule el bounding box bajo demanda.
			
			Envelope e = FConverter.convertRectangle2DtoEnvelope(geom.getBounds2D());
			spatialCache.insert(e, geom);
		}
	}
}
