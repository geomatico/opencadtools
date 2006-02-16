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
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.IOException;

import com.iver.cit.gvsig.CADExtension;
import com.iver.cit.gvsig.fmap.core.DefaultFeature;
import com.iver.cit.gvsig.fmap.core.FGeometryCollection;
import com.iver.cit.gvsig.fmap.core.FPoint2D;
import com.iver.cit.gvsig.fmap.core.FPolygon2D;
import com.iver.cit.gvsig.fmap.core.FPolyline2D;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.GeneralPathX;
import com.iver.cit.gvsig.fmap.core.GeneralPathXIterator;
import com.iver.cit.gvsig.fmap.core.Handler;
import com.iver.cit.gvsig.fmap.core.IFeature;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.IRow;
import com.iver.cit.gvsig.fmap.core.ShapeFactory;
import com.iver.cit.gvsig.fmap.core.v02.FGraphicUtilities;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.edition.IRowEdited;
import com.iver.cit.gvsig.fmap.edition.VectorialEditableAdapter;
import com.iver.cit.gvsig.fmap.layers.FBitSet;
import com.iver.cit.gvsig.gui.cad.DefaultCADTool;
import com.iver.cit.gvsig.gui.cad.tools.smc.EditVertexCADToolContext;
import com.iver.cit.gvsig.gui.cad.tools.smc.EditVertexCADToolContext.EditVertexCADToolState;


/**
 * DOCUMENT ME!
 *
 * @author Vicente Caballero Navarro
 */
public class EditVertexCADTool extends DefaultCADTool {
    private EditVertexCADToolContext _fsm;
    private int numSelect=0;
	private int numHandlers;

    /**
     * Crea un nuevo PolylineCADTool.
     */
    public EditVertexCADTool() {
    }

    /**
     * M�todo de incio, para poner el c�digo de todo lo que se requiera de una
     * carga previa a la utilizaci�n de la herramienta.
     */
    public void init() {
        _fsm = new EditVertexCADToolContext(this);
    }

    /* (non-Javadoc)
     * @see com.iver.cit.gvsig.gui.cad.CADTool#transition(com.iver.cit.gvsig.fmap.layers.FBitSet, double, double)
     */
    public void transition(double x, double y) {
        //_fsm.addPoint(x, y);
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

        if (selection.cardinality() == 0) {
            CADExtension.setCADTool("selection");
            ((SelectionCADTool) CADExtension.getCADToolAdapter().getCadTool()).setNextTool(
                "editvertex");
        }
    }

    /**
     * Equivale al transition del prototipo pero sin pasarle como par�metro el
     * editableFeatureSource que ya estar� creado.
     *
     * @param x par�metro x del punto que se pase en esta transici�n.
     * @param y par�metro y del punto que se pase en esta transici�n.
     */
    public void addPoint(double x, double y) {
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
        EditVertexCADToolState actualState = ((EditVertexCADToolContext) _fsm).getState();
        String status = actualState.getName();
        VectorialEditableAdapter vea = getCadToolAdapter().getVectorialAdapter();
        FBitSet selection = vea.getSelection();

        try {
            drawVertex(g, selection,
                getCadToolAdapter().getMapControl().getViewPort()
                    .getAffineTransform());
        } catch (DriverIOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Add a diferent option.
     *
     * @param s Diferent option.
     */
    public void addOption(String s) {
    	EditVertexCADToolState actualState = (EditVertexCADToolState) _fsm.getPreviousState();
        String status = actualState.getName();
        VectorialEditableAdapter vea = getCadToolAdapter().getVectorialAdapter();
        FBitSet selection = vea.getSelection();
        IRowEdited row=null;
        IGeometry ig=null;
        Handler[] handlers=null;
        if (selection.cardinality()==1){

			try {
				row = getCadToolAdapter().getVectorialAdapter().getRow(selection.nextSetBit(0));
			} catch (DriverIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	ig=((IFeature)row.getLinkedRow()).getGeometry().cloneGeometry();
        	handlers=ig.getHandlers(IGeometry.SELECTHANDLER);
        	numHandlers=handlers.length;
        	if (numHandlers ==0){
        		try {
					vea.removeRow(selection.nextSetBit(0),getName());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (DriverIOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        int dif=1;//En el caso de ser pol�gono.
        if (ig instanceof FGeometryCollection){
        	dif=2;
        }
        if (status.equals("EditVertex.SelectVertexOrDelete")){
        	if(s.equals("s") || s.equals("S") || s.equals("Siguiente")){
        		numSelect=numSelect-dif;
        		if (numSelect<0){
        			numSelect=numHandlers-1+(numSelect+1);
        		}
           }else if(s.equals("a") || s.equals("a") || s.equals("Anterior")){
        	   	numSelect=numSelect+dif;
       			if (numSelect>(numHandlers-1)){
       				numSelect=numSelect-(numHandlers);
       			}

        	}else if(s.equals("e") || s.equals("E") || s.equals("Eliminar")){
        		if (handlers!=null){
        			IGeometry newGeometry=removeVertex(ig,handlers[numSelect]);
        			numSelect=0;

        			IRow newRow=new DefaultFeature(newGeometry,row.getAttributes());
        			try {
						vea.modifyRow(selection.nextSetBit(0),newRow,getName());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (DriverIOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					getCadToolAdapter().getMapControl().drawMap(false);
        		}
        	}
        }
    }
    public void drawVertex(Graphics g,FBitSet sel,AffineTransform at) throws DriverIOException{
		 for (int i = sel.nextSetBit(0); i >= 0;
		 		i = sel.nextSetBit(i + 1)) {
			IGeometry ig = getCadToolAdapter().getVectorialAdapter().getShape(i).cloneGeometry();
			if (ig == null) continue;
				Handler[] handlers=ig.getHandlers(IGeometry.SELECTHANDLER);
				FGraphicUtilities.DrawVertex((Graphics2D)g,at,handlers[numSelect]);
		}
	}
    /* (non-Javadoc)
     * @see com.iver.cit.gvsig.gui.cad.CADTool#addvalue(double)
     */
    public void addValue(double d) {
    }
    public IGeometry removeVertex(IGeometry gp,Handler handler) {
        GeneralPathX newGp = new GeneralPathX();
        double[] theData = new double[6];

        GeneralPathXIterator theIterator;
        int theType;
        int numParts = 0;

        Point2D ptSrc = new Point2D.Double();
        boolean bFirst = false;

        theIterator = gp.getGeneralPathXIterator(); //, flatness);
        int numSegmentsAdded = 0;
        while (!theIterator.isDone()) {
            theType = theIterator.currentSegment(theData);
            if (bFirst){
        		newGp.moveTo(theData[0], theData[1]);
        		numSegmentsAdded++;
        		bFirst=false;
        		continue;
        	}
            switch (theType) {

                case PathIterator.SEG_MOVETO:
                    numParts++;
                    ptSrc.setLocation(theData[0], theData[1]);
                    if (ptSrc.equals(handler.getPoint())){
                    	numParts--;
                    	bFirst=true;
                    	break;
                    }
                    newGp.moveTo(ptSrc.getX(), ptSrc.getY());
                    numSegmentsAdded++;
                    bFirst = false;
                    break;

                case PathIterator.SEG_LINETO:
                    ptSrc.setLocation(theData[0], theData[1]);
                    if (ptSrc.equals(handler.getPoint())){
                    	break;
                    }
                    newGp.lineTo(ptSrc.getX(), ptSrc.getY());
                    bFirst = false;
                    numSegmentsAdded++;
                    break;

                case PathIterator.SEG_QUADTO:
                    newGp.quadTo(theData[0], theData[1], theData[2], theData[3]);
                    numSegmentsAdded++;
                    break;

                case PathIterator.SEG_CUBICTO:
                    newGp.curveTo(theData[0], theData[1], theData[2], theData[3], theData[4], theData[5]);
                    numSegmentsAdded++;
                    break;

                case PathIterator.SEG_CLOSE:
                    if (numSegmentsAdded < 3)
                        newGp.lineTo(theData[0], theData[1]);
                    newGp.closePath();

                    break;
            } //end switch

            theIterator.next();
        } //end while loop
        FShape shp = null;
        switch (gp.getGeometryType())
        {
            case FShape.POINT: //Tipo punto
            case FShape.POINT + FShape.Z:
                shp = new FPoint2D(ptSrc.getX(), ptSrc.getY());
                break;

            case FShape.LINE:
            case FShape.LINE + FShape.Z:
                shp = new FPolyline2D(newGp);
                break;
            case FShape.POLYGON:
            case FShape.POLYGON + FShape.Z:
                shp = new FPolygon2D(newGp);
                break;
        }
        return ShapeFactory.createGeometry(shp);
    }

	public String getName() {
		return "EDITAR VERTICE";
	}
}
