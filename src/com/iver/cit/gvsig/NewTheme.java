package com.iver.cit.gvsig;

import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jwizardcomponent.Utilities;
import jwizardcomponent.example.SimpleDynamicWizardPanel;
import jwizardcomponent.example.SimpleLabelWizardPanel;
import jwizardcomponent.frame.SimpleLogoJWizardFrame;

import com.hardcode.driverManager.Driver;
import com.hardcode.driverManager.DriverManager;
import com.hardcode.driverManager.WriterManager;
import com.iver.andami.PluginServices;
import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.fmap.drivers.ITableDefinition;
import com.iver.cit.gvsig.fmap.edition.ISpatialWriter;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.gui.View;
import com.iver.cit.gvsig.gui.cad.MyFinishAction;
import com.iver.cit.gvsig.gui.cad.panels.ChooseGeometryType;
import com.iver.cit.gvsig.gui.cad.panels.ChooseWriteDriver;
import com.iver.cit.gvsig.gui.cad.panels.JPanelFieldDefinition;
import com.iver.cit.gvsig.gui.cad.panels.ShpPanel;

/**
 * DOCUMENT ME!
 * 
 * @author Vicente Caballero Navarro
 */
public class NewTheme implements Extension {
	static ImageIcon LOGO;

	private ITableDefinition lyrDef;

	/**
	 * @see com.iver.andami.plugins.Extension#inicializar()
	 */
	public void inicializar() {
	}

	/**
	 * @see com.iver.andami.plugins.Extension#execute(java.lang.String)
	 */
public void execute(String actionCommand) {
		com.iver.andami.ui.mdiManager.View f = PluginServices.getMDIManager()
				.getActiveView();

		if (f instanceof View) {
			View vista = (View) f;

			LOGO = new javax.swing.ImageIcon(this.getClass().getClassLoader()
					.getResource("images/package_graphics.png"));
			// new
			// ImageIcon(DefaultJWizardComponents.class.getResource("images/logo.jpeg"));

			SimpleLogoJWizardFrame wizardFrame = new SimpleLogoJWizardFrame(
					LOGO);
			wizardFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			SwingUtilities.updateComponentTreeUI(wizardFrame);

			wizardFrame.setTitle("Creaci�n de un nuevo Tema");

		    DriverManager writerManager = LayerFactory.getDM(); 
		    ArrayList spatialDrivers = new ArrayList();
		    String[] writerNames = writerManager.getDriverNames();
			for (int i=0; i<writerNames.length; i++)
			{
				Driver drv = writerManager.getDriver(writerNames[i]);
				if (drv instanceof ISpatialWriter)
					spatialDrivers.add(drv.getName());
			}

			ChooseGeometryType panelChoose = new ChooseGeometryType(wizardFrame.getWizardComponents());
			JPanelFieldDefinition panelFields = new JPanelFieldDefinition(wizardFrame.getWizardComponents());			
			wizardFrame.getWizardComponents().addWizardPanel(panelChoose);

			wizardFrame.getWizardComponents().addWizardPanel(panelFields);

			if (actionCommand.equals("SHP"))
			{
				panelChoose.setDriver((ISpatialWriter) writerManager.getDriver("gvSIG shp driver"));
				wizardFrame.getWizardComponents().addWizardPanel(
					new ShpPanel(wizardFrame.getWizardComponents()));
				
				wizardFrame.getWizardComponents().setFinishAction(
						new MyFinishAction(wizardFrame.getWizardComponents(),
								vista.getMapControl(), actionCommand));
			}
			if (actionCommand.equals("DXF"))
			{
				wizardFrame.getWizardComponents().addWizardPanel(
					new SimpleLabelWizardPanel(wizardFrame
							.getWizardComponents(), new JLabel("Done!")));
			}
			if (actionCommand.equals("POSTGIS"))
			{
				wizardFrame.getWizardComponents().addWizardPanel(
					new SimpleLabelWizardPanel(wizardFrame
							.getWizardComponents(), new JLabel("Done!")));
			}			
			
			wizardFrame.setSize(540, 350);
			Utilities.centerComponentOnScreen(wizardFrame);
			wizardFrame.show();
			// System.out.println("Salgo con " + panelChoose.getLayerName());
		}
	}
	/**
	 * @see com.iver.andami.plugins.Extension#isEnabled()
	 */
	public boolean isEnabled() {
		View f = (View) PluginServices.getMDIManager().getActiveView();

		if (f == null) {
			return false;
		} else
			return true;
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

		if (f.getClass() == View.class) {
			return true;
		} else {
			return false;
		}

	}
}
