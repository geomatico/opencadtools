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
package com.iver.cit.gvsig;

import java.io.IOException;

import com.iver.andami.PluginServices;
import com.iver.andami.messages.NotificationManager;
import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.fmap.drivers.DriverIOException;
import com.iver.cit.gvsig.fmap.edition.IEditableSource;
import com.iver.cit.gvsig.fmap.edition.VectorialEditableAdapter;
import com.iver.cit.gvsig.gui.Table;


/**
 * Extensi�n encargada de gestionar el rehacer un comando anteriormente
 * deshecho.
 *
 * @author Vicente Caballero Navarro
 */
public class UndoTableExtension implements Extension {
	/**
	 * @see com.iver.andami.plugins.Extension#inicializar()
	 */
	public void inicializar() {
	}

	/**
	 * @see com.iver.andami.plugins.Extension#execute(java.lang.String)
	 */
	public void execute(String s) {
		Table tabla = (Table) PluginServices.getMDIManager().getActiveView();

		if (s.compareTo("UNDO") == 0) {

						if (tabla.isEditing()){
							IEditableSource vea=(IEditableSource)tabla.getModel().getModelo();
							try {
								vea.undo();
							} catch (DriverIOException e) {
								NotificationManager.addError("Error accediendo a los Drivers para deshacer un comando",
										e);
							} catch (IOException e) {
								NotificationManager.addError("Error accediendo a los Drivers para deshacer un comando",
										e);
							}
							vea.getSelection().clear();
						}



		}
	}

	/**
	 * @see com.iver.andami.plugins.Extension#isEnabled()
	 */
	public boolean isEnabled() {
		Table tabla = (Table) PluginServices.getMDIManager().getActiveView();
		//MapControl mapControl = (MapControl) vista.getMapControl();
		//FLayers layers=mapControl.getMapContext().getLayers();
		//for (int i=0;i<layers.getLayersCount();i++){
			if (tabla.getModel().getModelo() instanceof IEditableSource && tabla.isEditing()){
				IEditableSource vea=(IEditableSource)tabla.getModel().getModelo();
				if (vea==null)return false;
				return vea.moreUndoCommands();
			}

		//}
		return false;
	}

	/**
	 * @see com.iver.andami.plugins.Extension#isVisible()
	 */
	public boolean isVisible() {
		com.iver.andami.ui.mdiManager.View f = PluginServices.getMDIManager()
															 .getActiveView();

		if (f == null) {
			return false;
		}

		if (f.getClass() == Table.class) {
			return true;
		} else {
			return false;
		}
	}
}