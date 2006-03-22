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
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;

import com.iver.andami.PluginServices;
import com.iver.cit.gvsig.CADExtension;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.UtilFunctions;
import com.iver.cit.gvsig.fmap.edition.VectorialEditableAdapter;
import com.iver.cit.gvsig.fmap.layers.FBitSet;
import com.iver.cit.gvsig.gui.cad.CADTool;
import com.iver.cit.gvsig.gui.cad.DefaultCADTool;
import com.iver.cit.gvsig.gui.cad.tools.smc.MoveCADToolContext;
import com.iver.cit.gvsig.gui.cad.tools.smc.MoveCADToolContext.MoveCADToolState;


/**
 * DOCUMENT ME!
 *
 * @author Vicente Caballero Navarro
 */
public class MoveCADTool extends DefaultCADTool {
    private MoveCADToolContext _fsm;
    private Point2D firstPoint;
    private Point2D lastPoint;

    /**
     * Crea un nuevo PolylineCADTool.
     */
    public MoveCADTool() {
    }

    /**
     * M�todo de incio, para poner el c�digo de todo lo que se requiera de una
     * carga previa a la utilizaci�n de la herramienta.
     */
    public void init() {
        _fsm = new MoveCADToolContext(this);
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
        //_fsm.addValue(sel,d);
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
        FBitSet selection = CADExtension.getCADToolAdapter()
                                        .getVectorialAdapter().getSelection();

        if (selection.cardinality() == 0 && !CADExtension.getCADToolAdapter().getCadTool().getClass().getName().equals("com.iver.cit.gvsig.gui.cad.tools.SelectionCADTool")) {
            CADExtension.setCADTool("selection");
            ((SelectionCADTool) CADExtension.getCADToolAdapter().getCadTool()).setNextTool(
                "move");
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
        MoveCADToolState actualState = (MoveCADToolState) _fsm.getPreviousState();
        String status = actualState.getName();
        VectorialEditableAdapter vea = getCadToolAdapter().getVectorialAdapter();
        ArrayList selectedRow=getSelectedRow();

    	if (status.equals("Move.FirstPointToMove")) {
            firstPoint = new Point2D.Double(x, y);
        } else if (status.equals("Move.SecondPointToMove")) {
            PluginServices.getMDIManager().setWaitCursor();
            lastPoint = new Point2D.Double(x, y);
            vea.startComplexRow();

            try {
              for (int i = 0; i < selectedRow.size(); i++) {
        			IRowEdited edRow = (IRowEdited) selectedRow.get(i);
        			IFeature feat = (IFeature) edRow.getLinkedRow().cloneRow();
        			IGeometry ig = feat.getGeometry();
        			if (ig == null)
        				continue;
        			 // Movemos la geometr�a
                    UtilFunctions.moveGeom(ig, lastPoint.getX() -
                            firstPoint.getX(), lastPoint.getY() - firstPoint.getY());

                    vea.modifyRow(edRow.getIndex(),feat,getName());
        		}
              FBitSet selection = CADExtension.getCADToolAdapter()
              		.getVectorialAdapter().getSelection();
              	selection.clear();
                selectedRow.clear();
                vea.endComplexRow();
            } catch (DriverIOException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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
        MoveCADToolState actualState = ((MoveCADToolContext) _fsm).getState();
        String status = actualState.getName();
        ArrayList selectedRow=getSelectedRow();
        	drawHandlers(g, selectedRow,
                     getCadToolAdapter().getMapControl().getViewPort()
                         .getAffineTransform());
        if (status.equals("Move.SecondPointToMove")) {
            for (int i = 0; i < selectedRow.size(); i++) {
    			IRowEdited edRow = (IRowEdited) selectedRow.get(i);
    			IFeature feat = (IFeature) edRow.getLinkedRow();
    			IGeometry ig = feat.getGeometry().cloneGeometry();
    			if (ig == null)
    				continue;
    			// Movemos la geometr�a
                UtilFunctions.moveGeom(ig, x - firstPoint.getX(), y - firstPoint.getY());
                ig.draw((Graphics2D) g,
                    getCadToolAdapter().getMapControl().getViewPort(),
                    CADTool.drawingSymbol);
    		}
        }
    }

    /**
     * Add a diferent option.
     *
     * @param s Diferent option.
     */
    public void addOption(String s) {
    }

    /* (non-Javadoc)
     * @see com.iver.cit.gvsig.gui.cad.CADTool#addvalue(double)
     */
    public void addValue(double d) {
    }

	public String getName() {
		return "DESPLAZAR";
	}
}
