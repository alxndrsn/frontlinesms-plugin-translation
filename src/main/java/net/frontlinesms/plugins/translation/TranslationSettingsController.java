package net.frontlinesms.plugins.translation;

import net.frontlinesms.plugins.PluginSettingsController;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

public class TranslationSettingsController implements ThinletUiEventHandler, PluginSettingsController {

	private UiGeneratorController uiController;
	private TranslationPluginController pluginController;

	public TranslationSettingsController(TranslationPluginController pluginController, UiGeneratorController uiController) {
		this.pluginController = pluginController;
		this.uiController = uiController;
	}


	public String getTitle() {
		return this.pluginController.getName(InternationalisationUtils.getCurrentLocale());
	}
	
	public Object getRootSettingsNode() {
		Object rootSettingsNode = this.uiController.createNode(getTitle(), this.pluginController.getClass().toString());
		this.uiController.add(rootSettingsNode, this.uiController.createNode("Test", "test"));
		
		return rootSettingsNode;
	}
	
	public Object getPanelForSection(String section) {
		switch (HttpTriggerSettingsSections.valueOf(section)) {
			case APPEARANCE:
				return getAppearancePanel();
			case TEST:
				return getTestPanel();
			default:
				return null;
		}
	}

	private Object getTestPanel() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Object getAppearancePanel() {
		// TODO Auto-generated method stub
		return null;
	}
	
	enum HttpTriggerSettingsSections {
		APPEARANCE,
		TEST
	}

}
