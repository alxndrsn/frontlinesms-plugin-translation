/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.plugins.translation.ui;

import java.io.IOException;

import net.frontlinesms.plugins.translation.MasterTranslationFile;
import net.frontlinesms.plugins.translation.TranslationThinletTabController;
import net.frontlinesms.ui.EnumCountry;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * This class is responsible for showing the New/Edit translation dialog and handling its events.
 * 
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
@TextResourceKeyOwner(prefix={"MESSAGE_", "I18N"})
public class LanguagePropertiesHandler implements ThinletUiEventHandler {
	
//> UI LAYOUT FILE PATHS
	/** [ui layout file path] The language selection page */
	private static final String UI_FILE_NEW_TRANSLATION = "/ui/plugins/translation/dgLanguageProperties.xml";
	
//> I18N
	private static final String I18N_LANGUAGE_ALREADY_TRANSLATED = "plugins.translation.language.already.translated";
	private static final String I18N_CONFIRM_SAVE_ALL = "plugins.translation.warning.save.all";

//> UI COMPONENT NAMES
	private static final String UI_COMPONENT_BT_SAVE = "btSave";
	private static final String UI_COMPONENT_CHECKBOX_BASE_LANGUAGE = "cbBaseLanguage";
	private static final String UI_COMPONENT_CHECKBOX_FONT = "cbFont";
	private static final String UI_COMPONENT_COMBOBOX_COUNTRIES = "cbCountries";
	private static final String UI_COMPONENT_COMBOBOX_KNOWN_LANGUAGES = "cbKnownLanguages";
	private static final String UI_COMPONENT_TF_LANGUAGE_NAME = "tfLanguageName";
	private static final String UI_COMPONENT_TF_FONT = "tfFont";
	private static final String UI_COMPONENT_TF_ISO_CODE = "tfISOCode";

//> INSTANCE VARIABLES
	private MasterTranslationFile originalLanguageBundle;
	private Object dialogComponent;
	private UiGeneratorController ui;
	private TranslationThinletTabController owner;

//> CONSTRUCTORS
	/**
	 * Language Properties Handler
	 */
	public LanguagePropertiesHandler(UiGeneratorController ui, TranslationThinletTabController translationThinletTabController) {
		this.ui = ui;
		this.owner = translationThinletTabController;
		this.dialogComponent = this.ui.loadComponentFromFile(UI_FILE_NEW_TRANSLATION, this);
	}
	
	/**
	 * @return the instance of the language selection dialog 
	 */
	public Object getDialog() {
		return this.dialogComponent;
	}
	
	/**
	 * Initializes the dialog
	 */
	public void initDialog() {
		this.populateCountryFlags();
		this.populateKnownLanguages();
		
		this.ui.setVisible(find(UI_COMPONENT_CHECKBOX_BASE_LANGUAGE), this.originalLanguageBundle == null);
		this.ui.setVisible(find(UI_COMPONENT_COMBOBOX_KNOWN_LANGUAGES), this.originalLanguageBundle == null);
	}

	/** @see UiGeneratorController#removeDialog(Object) */
	public void removeDialog() {
		this.ui.removeDialog(dialogComponent);
	}
	
	/**
	 * Open the browser to display a web page
	 * @param url
	 */
	public void openBrowser(String url) {
		this.ui.openBrowser(url);
	}
	
	/**
	 * Populate the dialog with the properties of a language bundle
	 * @param languageBundle The selected bundle being edited
	 */
	public void populate(MasterTranslationFile languageBundle) {
		Object tfLanguageName = find(UI_COMPONENT_TF_LANGUAGE_NAME);
		Object tfIsoCode = find(UI_COMPONENT_TF_ISO_CODE);
		Object cbFont = find(UI_COMPONENT_CHECKBOX_FONT);
		Object tfFont = find(UI_COMPONENT_TF_FONT);
		
		this.ui.setText(tfLanguageName, languageBundle.getLanguageName());
		this.ui.setText(tfIsoCode, languageBundle.getLanguageCode());
		
		String fonts = languageBundle.getLanguageFont();
		if (fonts != null) {
			this.ui.setText(tfFont, languageBundle.getLanguageFont());
		}
		
		this.ui.setSelected(cbFont, fonts != null);
		this.ui.setEnabled(tfFont, fonts != null);
		
		this.originalLanguageBundle = languageBundle;
	}

//> UI METHODS
	
	/**
	 * Prepares the new translation
	 * @param list
	 * @throws IOException 
	 */
	public void saveProperties(String languageName, String isoCode, Object countryList) throws IOException {
		if (this.isAllOkForNewTranslation(languageName, isoCode)) {
			String countryCode = this.ui.getAttachedObject(this.ui.getSelectedItem(countryList), String.class);
			if (isLanguageAndCountryAlreadyPresent(isoCode, countryCode)) {
				this.ui.alert(InternationalisationUtils.getI18nString(I18N_LANGUAGE_ALREADY_TRANSLATED));
			} else {
				if (this.originalLanguageBundle == null) {
					this.doSaveProperties();
				} else {
					this.ui.showConfirmationDialog("doSaveProperties", this, I18N_CONFIRM_SAVE_ALL);
				}
			}
		}
	}
	
	/**
	 * Creates the new translation
	 * @param list
	 * @throws IOException 
	 */
	public void doSaveProperties() throws IOException {
		this.ui.removeConfirmationDialog();
		
		String languageName = this.ui.getText(find(UI_COMPONENT_TF_LANGUAGE_NAME));
		String isoCode = this.ui.getText(find(UI_COMPONENT_TF_ISO_CODE));
		String countryCode = this.ui.getAttachedObject(this.ui.getSelectedItem(find(UI_COMPONENT_COMBOBOX_COUNTRIES))).toString();
		
		// Not required
		Object baseLanguageCode = (this.ui.isSelected(find(UI_COMPONENT_CHECKBOX_BASE_LANGUAGE)) ? this.ui.getSelectedItem(find(UI_COMPONENT_COMBOBOX_KNOWN_LANGUAGES)) : null);
		String fontNames = (this.ui.isSelected(find(UI_COMPONENT_CHECKBOX_FONT)) ? this.ui.getText(find(UI_COMPONENT_TF_FONT)) : null);
		
		// If this is a new language
		if (this.originalLanguageBundle == null) {
				this.owner.createNewLanguageFile(languageName, isoCode, countryCode, baseLanguageCode, fontNames, getFileName(isoCode, countryCode));
		} else {
			this.owner.updateTranslationFile(this.originalLanguageBundle, languageName, isoCode, countryCode, fontNames, getFileName(isoCode, countryCode));
		}
		this.removeDialog();
	}

	private String getFileName(String isoCode, String countryCode) {
		return "frontlineSMS_" + isoCode + (isLanguageAlreadyPresent(isoCode) ? "_" + countryCode.toUpperCase() : "") + ".properties";
	}

	/**
	 * Checks if the language is already translated, taking the country code into account
	 * @param isoCode ISO-639-1 Code of the language
	 * @return <code>true</code> if the language is already translated, <code>false</code> otherwise.
	 */
	public boolean isLanguageAndCountryAlreadyPresent(String isoCode, String countryCode) {
		for(MasterTranslationFile languageBundle : MasterTranslationFile.getAll()) {
			// If the code is already present, we return true
			// But if the given ISO Code is the one that had been populated, it means that we are just editing this language
			if (languageBundle.getLanguageCode().equals(isoCode)
					&& languageBundle.getCountry().equals(countryCode) 
					&& (this.originalLanguageBundle == null
					|| (!isoCode.equals(this.originalLanguageBundle.getLanguageCode())
					|| !countryCode.equals(this.originalLanguageBundle.getCountry())))) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the language is already translated
	 * @param isoCode ISO-639-1 Code of the language
	 * @return <code>true</code> if the language is already translated, <code>false</code> otherwise.
	 */
	public boolean isLanguageAlreadyPresent(String isoCode) {
		for(MasterTranslationFile languageBundle : MasterTranslationFile.getAll()) {
			// If the code is already present, we return true
			// But if the given ISO Code is the one that had been populated, it means that we are just editing this language
			if (languageBundle.getLanguageCode().equals(isoCode)
					&& (this.originalLanguageBundle == null
					|| !languageBundle.getFilename().equals(this.originalLanguageBundle.getFilename()))) {
				return true;
			}
		}
		
		return false;
	}

//> INSTANCE HELPER METHODS
	/** Populate and display the countries in a Combo Box. */
	private void populateCountryFlags() {
		Object countryList = find(UI_COMPONENT_COMBOBOX_COUNTRIES);
		int selectedIndex = -1;
		// Missing translation files
		for (int i = 0 ; i < EnumCountry.values().length ; ++i) {
			EnumCountry enumCountry = EnumCountry.values()[i];
			
			Object comboBoxChoice = this.ui.createComboboxChoice(enumCountry.getEnglishName(), enumCountry.getCode());
			this.ui.setIcon(comboBoxChoice, this.ui.getFlagIcon(enumCountry.getCode()));
			
			this.ui.add(countryList, comboBoxChoice);
			if (this.originalLanguageBundle != null && this.originalLanguageBundle.getCountry().equals(enumCountry.getCode())) {
				selectedIndex = i;
			}
		}
		
		this.ui.setSelectedIndex(countryList, selectedIndex);
	}
	
	/**
	 * Fill the Combobox displaying potentials base languages
	 */
	private void populateKnownLanguages() {
		Object knownLanguages = find(UI_COMPONENT_COMBOBOX_KNOWN_LANGUAGES);
		
		for (MasterTranslationFile languageBundle : MasterTranslationFile.getAll()) {
			Object comboBoxChoice = this.ui.createComboboxChoice(languageBundle.getLanguageName(), languageBundle.getLanguageCode());
			this.ui.setIcon(comboBoxChoice, this.ui.getFlagIcon(languageBundle));
			this.ui.add(knownLanguages, comboBoxChoice);
		}
	}
	
	/**
	 * UI event triggered when either the language name field or the ISO code field text changed
	 * @param languageName The content of the Language Name Field
	 * @param isoCode The content of the ISO Code Field
	 */
	public void fieldChanged () {
		Object tfLanguageName = find(UI_COMPONENT_TF_LANGUAGE_NAME);
		Object tfIsoCode = find(UI_COMPONENT_TF_ISO_CODE);
		
		String languageName = this.ui.getText(tfLanguageName);
		String isoCode = this.ui.getText(tfIsoCode);
		
		boolean enableSaveButton = this.isAllOkForNewTranslation(languageName, isoCode);
		
		this.ui.setEnabled(find(UI_COMPONENT_BT_SAVE), enableSaveButton);
	}

	/**
	 * Checks whether the filled component are enough to let the user save the properties
	 * @param languageName The content of the Language Name Field
	 * @param isoCode The content of the ISO Code Field
	 * @return
	 */
	private boolean isAllOkForNewTranslation(String languageName, String isoCode) {
		return (languageName != null 
				&& isoCode != null
				&& find(UI_COMPONENT_COMBOBOX_COUNTRIES) != null
				&& languageName.length() > 0
				&& isoCode.length() > 0
				&& this.ui.getSelectedIndex(find(UI_COMPONENT_COMBOBOX_COUNTRIES)) >= 0);
	}
	
	/**
	 * UI Event triggered when the user checks or unchecks the "Base language" checkbox
	 * @param isSelected <code>true</code> if the Checkbox is checked, <code>false</code> otherwise
	 */
	public void checkboxBaseChanged(boolean isSelected) {
		Object cbKnownLanguages = find(UI_COMPONENT_COMBOBOX_KNOWN_LANGUAGES);
		this.ui.setEnabled(cbKnownLanguages, isSelected);
	}
	
	/**
	 * UI Event triggered when the user checks or unchecks the "Font" checkbox
	 * @param isSelected <code>true</code> if the Checkbox is checked, <code>false</code> otherwise
	 */
	public void checkboxFontChanged(boolean isSelected) {
		Object tfFont = find(UI_COMPONENT_TF_FONT);
		this.ui.setEnabled(tfFont, isSelected);
		
		this.fieldChanged();
	}
	
	/** @see UiGeneratorController#showHelpPage(String) */
	public void showHelpPage(String page) {
		this.ui.showHelpPage(page);
	}

	private Object find (String component) {
		return this.ui.find(this.dialogComponent, component);
	}
}
