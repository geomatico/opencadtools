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
package com.iver.cit.gvsig;

import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.iver.andami.PluginServices;
import com.iver.andami.messages.NotificationManager;
import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.fmap.layers.FLyrVect;
import com.iver.cit.gvsig.gui.cad.CADToolAdapter;
import com.iver.cit.gvsig.gui.cad.tools.CutPolygonCADTool;
import com.iver.cit.gvsig.gui.cad.tools.CutRedigitalizeCommons;
import com.iver.cit.gvsig.layers.ILayerEdited;

/**
 * Extension to cut polygons on edition
 * 
 * @author Jose Ignacio Lamas
 * @author Nacho Varela [Cartolab]
 * @author Pablo Sanxiao [Cartolab]
 */
public class CutPolygonExtension extends Extension {

    private CutRedigitalizeCommons tool;

    private final String iconPath = "images/icons/cortar_area.png";
    private final String iconCode = "edition-geometry-cut-polygon";
    private final String toolCode = "_cut_polygon";

    @Override
    public void initialize() {
	tool = new CutPolygonCADTool();
	CADToolAdapter.addCADTool(toolCode, tool);
	registerIcon();
    }

    private void registerIcon() {
	PluginServices.getIconTheme().registerDefault(iconCode,
		this.getClass().getClassLoader().getResource(iconPath));
    }

    @Override
    public boolean isVisible() {
	return EditionUtilities.getEditionStatus() == EditionUtilities.EDITION_STATUS_ONE_VECTORIAL_LAYER_ACTIVE_AND_EDITABLE;
    }

    @Override
    public boolean isEnabled() {

	if (EditionUtilities.getEditionStatus() == EditionUtilities.EDITION_STATUS_ONE_VECTORIAL_LAYER_ACTIVE_AND_EDITABLE) {

	    ILayerEdited iLayerEdited = CADExtension.getEditionManager()
		    .getActiveLayerEdited();
	    if (iLayerEdited != null) {

		FLyrVect lv = (FLyrVect) iLayerEdited.getLayer();

		try {
		    if (tool.isApplicable(lv.getShapeType())) {
			return true;
		    }
		} catch (ReadDriverException e) {
		    NotificationManager.addError(e.getMessage(), e);
		}
	    }
	}

	return false;
    }

    @Override
    public void execute(String s) {
	CADExtension.initFocus();
	if (s.equals(toolCode)) {
	    CADExtension.setCADTool(toolCode, true);
	}
	CADExtension.getCADToolAdapter().configureMenu();
    }

}
