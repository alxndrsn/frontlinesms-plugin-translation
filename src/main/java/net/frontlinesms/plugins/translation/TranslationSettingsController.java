package net.frontlinesms.plugins.translation;

import net.frontlinesms.plugins.PluginSettingsController;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

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
	
	public void addSubSettingsNodes(Object rootSettingsNode) {
	}
	
	public Object getRootPanel() {
		return null;
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
	
	public FrontlineValidationMessage validateFields() {
		return null;
	}
	
	enum HttpTriggerSettingsSections {
		APPEARANCE,
		TEST
	}

	public UiSettingsSectionHandler getHandlerForSection(String section) {
		return null;
	}

	public UiSettingsSectionHandler getRootPanelHandler() {
		return null;
	}

}
