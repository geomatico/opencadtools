package com.iver.cit.gvsig;

import com.iver.andami.PluginServices;
import com.iver.andami.ui.mdiManager.View;
import com.iver.cit.gvsig.fmap.edition.AfterRowEditEvent;
import com.iver.cit.gvsig.fmap.edition.BeforeRowEditEvent;
import com.iver.cit.gvsig.fmap.edition.EditionEvent;
import com.iver.cit.gvsig.fmap.edition.IEditionListener;
import com.iver.cit.gvsig.fmap.layers.FLayer;
import com.iver.cit.gvsig.fmap.layers.FLayers;
import com.iver.cit.gvsig.gui.Table;

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
/* CVS MESSAGES:
 *
 * $Id$
 * $Log$
 * Revision 1.3  2006-05-10 06:26:24  caballero
 * comprobar si tiene capa asociada
 *
 * Revision 1.2  2006/05/09 09:26:04  caballero
 * refrescar las vistas y tablas
 *
 * Revision 1.1  2006/05/05 09:06:09  jorpiell
 * Se a a�adido la clase EditionChangeManager, que no es m�s que un listener que se ejecuta cuando se produce un evento de edici�n.
 *
 *
 */
/**
 * Cuando un tema se pone en edici�n se le debe asociar
 * un listener de este tipo, que se dispar� cuando se produzca
 * un evento de edici�n (borrado, modificaci�n,... sobre la capa.
 *
 * @author Jorge Piera Llodr� (piera_jor@gva.es)
 */
public class EditionChangeManager implements IEditionListener{
	private FLayer fLayer = null;

	/**
	 * Constructor
	 * @param fLayer
	 * Tema que se est� editando
	 */
	public EditionChangeManager(FLayer fLayer){
		this.fLayer = fLayer;
	}
	/*
	 *  (non-Javadoc)
	 * @see com.iver.cit.gvsig.fmap.edition.IEditionListener#processEvent(com.iver.cit.gvsig.fmap.edition.EditionEvent)
	 */
	public void processEvent(EditionEvent e) {
		// TODO Auto-generated method stub

	}

	/*
	 *  (non-Javadoc)
	 * @see com.iver.cit.gvsig.fmap.edition.IEditionListener#beforeRowEditEvent(com.iver.cit.gvsig.fmap.edition.BeforeRowEditEvent)
	 */
	public void beforeRowEditEvent(BeforeRowEditEvent e) {
		// TODO Auto-generated method stub

	}

	/*
	 *  (non-Javadoc)
	 * @see com.iver.cit.gvsig.fmap.edition.IEditionListener#afterRowEditEvent(com.iver.cit.gvsig.fmap.edition.AfterRowEditEvent)
	 */
	public void afterRowEditEvent(AfterRowEditEvent e) {
		View[] views = (View[]) PluginServices.getMDIManager().getAllViews();

		for (int i=0 ; i<views.length ; i++){
			if (views[i] instanceof Table){
				Table table=(Table)views[i];
				if (table.getModel().getAssociatedTable()!=null && table.getModel().getAssociatedTable().equals(fLayer))
					table.refresh();
			}else if (views[i] instanceof com.iver.cit.gvsig.gui.View){
				com.iver.cit.gvsig.gui.View view=(com.iver.cit.gvsig.gui.View)views[i];
				FLayers layers=view.getMapControl().getMapContext().getLayers();
				for (int j=0;j<layers.getLayersCount();j++){
					if (layers.getLayer(j).equals(fLayer)){
						view.repaintMap();
					}
				}
			}
		}

	}

}