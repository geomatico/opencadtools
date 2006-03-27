/* gvSIG. Sistema de Informaci�n Geogr�fica de la Generalitat Valenciana
 *
 * Copyright (C) 2004 IVER T.I. and Generalitat Valenciana.
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
 *
 * For more information, contact:
 *
 *  Generalitat Valenciana
 *   Conselleria d'Infraestructures i Transport
 *   Av. Blasco Ib��ez, 50
 *   46010 VALENCIA
 *   SPAIN
 *
 *      +34 963862235
 *   gvsig@gva.es
 *      www.gvsig.gva.es
 *
 *    or
 *
 *   IVER T.I. S.A
 *   Salamanca 50
 *   46005 Valencia
 *   Spain
 *
 *   +34 963163400
 *   dac@iver.es
 */
package com.iver.cit.gvsig.gui.cad.tools;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;

import com.iver.andami.PluginServices;
import com.iver.cit.gvsig.CADExtension;
import com.iver.cit.gvsig.fmap.core.DefaultFeature;
import com.iver.cit.gvsig.fmap.core.GeneralPathX;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.ShapeFactory;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.edition.DefaultRowEdited;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.UtilFunctions;
import com.iver.cit.gvsig.gui.cad.CADTool;
import com.iver.cit.gvsig.gui.cad.DefaultCADTool;
import com.iver.cit.gvsig.gui.cad.tools.smc.RotateCADToolContext;
import com.iver.cit.gvsig.gui.cad.tools.smc.RotateCADToolContext.RotateCADToolState;


/**
 * DOCUMENT ME!
 *
 * @author Vicente Caballero Navarro
 */
public class RotateCADTool extends DefaultCADTool {
    private RotateCADToolContext _fsm;
    private Point2D firstPoint;
    private Point2D lastPoint;

    /**
     * Crea un nuevo PolylineCADTool.
     */
    public RotateCADTool() {
    }

    /**
     * M�todo de incio, para poner el c�digo de todo lo que se requiera de una
     * carga previa a la utilizaci�n de la herramienta.
     */
    public void init() {
        _fsm = new RotateCADToolContext(this);
    }

    /* (non-Javadoc)
     * @see com.iver.cit.gvsig.gui.cad.CADTool#transition(com.iver.cit.gvsig.fmap.layers.FBitSet, double, double)
     */
    public void transition(double x, double y, InputEvent event) {
        _fsm.addPoint(x, y, event);
    }

    /* (non-Javadoc)
     * @see com.iver.cit.gvsig.gui.cad.CADTool#transition(com.iver.cit.gvsig.fmap.layers.FBitSet, double)
     */
    public void transition(double d) {
        _fsm.addValue(d);
    }

    /* (non-Javadoc)
     * @see com.iver.cit.gvsig.gui.cad.CADTool#transition(com.iver.cit.gvsig.fmap.layers.FBitSet, java.lang.String)
     */
    public void transition(String s) {
        _fsm.addOption(s);
    }

    /**
     * DOCUMENT ME!
     */
    public void selection() {
       ArrayList selectedRows=getSelectedRows();
        if (selectedRows.size() == 0 && !CADExtension.getCADTool().getClass().getName().equals("com.iver.cit.gvsig.gui.cad.tools.SelectionCADTool")) {
            CADExtension.setCADTool("selection");
            ((SelectionCADTool) CADExtension.getCADTool()).setNextTool(
                "rotate");
        }
    }

    /**
     * Equivale al transition del prototipo pero sin pasarle como par�metro el
     * editableFeatureSource que ya estar� creado.
     *
     * @param x par�metro x del punto que se pase en esta transici�n.
     * @param y par�metro y del punto que se pase en esta transici�n.
     */
    public void addPoint(double x, double y,InputEvent event) {
        RotateCADToolState actualState = (RotateCADToolState) _fsm.getPreviousState();
        String status = actualState.getName();
        ArrayList selectedRow=getSelectedRows();

        if (status.equals("Rotate.PointMain")) {
        	firstPoint = new Point2D.Double(x, y);
    		} else if (status.equals("Rotate.AngleOrPoint")) {
    			PluginServices.getMDIManager().setWaitCursor();
    			lastPoint = new Point2D.Double(x,y);

    			double w;
    			double h;
    			w = lastPoint.getX() - firstPoint.getX();
    			h = lastPoint.getY() - firstPoint.getY();

    			try {
				getCadToolAdapter().getVectorialAdapter().startComplexRow();

				for (int i = 0; i < selectedRow.size(); i++) {
					DefaultFeature fea = (DefaultFeature) ((DefaultRowEdited) selectedRow
							.get(i)).getLinkedRow().cloneRow();
					// Rotamos la geometry
					UtilFunctions.rotateGeom(fea.getGeometry(), -Math.atan2(w,
							h)
							+ (Math.PI / 2), firstPoint.getX(), firstPoint
							.getY());

					getCadToolAdapter().getVectorialAdapter().modifyRow(i, fea,
							getName());
				}
				clearSelection();
				getCadToolAdapter().getVectorialAdapter().endComplexRow();
			} catch (DriverIOException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

    			PluginServices.getMDIManager().restoreCursor();
    		}
    }

    /**
     * M�todo para dibujar la lo necesario para el estado en el que nos
     * encontremos.
     *
     * @param g Graphics sobre el que dibujar.
     * @param x par�metro x del punto que se pase para dibujar.
     * @param y par�metro x del punto que se pase para dibujar.
     */
    public void drawOperation(Graphics g, double x, double y) {
        RotateCADToolState actualState = ((RotateCADToolContext) _fsm).getState();
        String status = actualState.getName();
        ArrayList selectedRow=getSelectedRows();

        drawHandlers(g, selectedRow,
                 getCadToolAdapter().getMapControl().getViewPort()
                     .getAffineTransform());
        if (status.equals("Rotate.AngleOrPoint")) {
			double w;
			double h;
			w = x - firstPoint.getX();
			h = y - firstPoint.getY();

			///AffineTransform at = AffineTransform.getRotateInstance(+Math.atan2(
			///			w, h) - (Math.PI / 2), (int) point.getX(),
			///		(int) point.getY());
			///Image img = getCadToolAdapter().getVectorialAdapter().getImage();

			///((Graphics2D) g).drawImage(img, at, null);

			///drawLine((Graphics2D) g, firstPoint, new Point2D.Double(x, y));


			     for (int i = 0; i < selectedRow.size(); i++) {
			                   // IGeometry geometry =
								// getCadToolAdapter().getVectorialAdapter().getShape(i);
				IRowEdited edRow = (IRowEdited) selectedRow.get(i);
				IFeature feat = (IFeature) edRow.getLinkedRow();
				IGeometry geometry = feat.getGeometry().cloneGeometry();
				// Rotamos la geometry
				UtilFunctions.rotateGeom(geometry, -Math.atan2(w, h) + Math.PI
						/ 2, firstPoint.getX(), firstPoint.getY());

				geometry.draw((Graphics2D) g, getCadToolAdapter()
						.getMapControl().getViewPort(), CADTool.drawingSymbol);
				GeneralPathX elShape = new GeneralPathX(
						GeneralPathX.WIND_EVEN_ODD, 2);
				elShape.moveTo(firstPoint.getX(), firstPoint.getY());
				elShape.lineTo(x, y);
				ShapeFactory.createPolyline2D(elShape).draw((Graphics2D) g,
						getCadToolAdapter().getMapControl().getViewPort(),
						CADTool.drawingSymbol);

			}
		}
    }

    /**
	 * Add a diferent option.
	 *
	 * @param s
	 *            Diferent option.
	 */
    public void addOption(String s) {
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see com.iver.cit.gvsig.gui.cad.CADTool#addvalue(double)
	 */
    public void addValue(double d) {
    	RotateCADToolState actualState = (RotateCADToolState) _fsm.getPreviousState();
        String status = actualState.getName();
        ArrayList selectedRow=getSelectedRows();

    	if (status.equals("Rotate.AngleOrPoint")) {
			try {
				for (int i = 0; i < selectedRow.size(); i++) {
					DefaultFeature fea = (DefaultFeature) ((DefaultRowEdited) selectedRow
							.get(i)).getLinkedRow().cloneRow();
						// Rotamos la geometry
						AffineTransform at = new AffineTransform();
						at.rotate(Math.toRadians(d),
	    						firstPoint.getX(), firstPoint.getY());
						fea.getGeometry().transform(at);
    					getCadToolAdapter().getVectorialAdapter().modifyRow(i, fea,getName());

				}
				clearSelection();
			} catch (DriverIOException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}
    }

	public String getName() {
		return PluginServices.getText(this,"rotate_");
	}
}
