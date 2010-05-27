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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.plugins.translation.MasterTranslationFile;
import net.frontlinesms.plugins.translation.TranslationThinletTabController;
import net.frontlinesms.resources.ResourceUtils;
import net.frontlinesms.ui.EnumCountry;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.LanguageBundle;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * This class is responsible for showing the New/Edit translation dialog and handling its events.
 * 
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
@TextResourceKeyOwner(prefix={"MESSAGE_", "I18N"})
public class NewTranslationHandler implements ThinletUiEventHandler {
	
//> UI LAYOUT FILE PATHS
	/** [ui layout file path] The language selection page */
	private static final String UI_FILE_NEW_TRANSLATION = "/ui/plugins/translation/dgAddTranslation.xml";

	private static final String I18N_BAD_ISO_CODE = "plugins.translation.bad.iso.code";
	private static final String I18N_LANGUAGE_ALREADY_TRANSLATED = "plugins.translation.language.already.translated";
	private static final String I18N_TRANSLATION_SAVED = "plugins.translation.translation.file.saved";

//> UI COMPONENT NAMES
	private static final String COMPONENT_BT_SAVE = "btSave";
	private static final String COMPONENT_COMBOBOX_COUNTRIES = "cbCountries";
	private static final String COMPONENT_TF_LANGUAGE_NAME = "tfLanguageName";
	private static final String COMPONENT_TF_ISO_CODE = "tfISOCode";


		
//> INSTANCE VARIABLES
	private Object dialogComponent;
	private UiGeneratorController ui;

	private TranslationThinletTabController owner;

	private MasterTranslationFile originalLanguageBundle;


//> CONSTRUCTORS
	/**
	 * New Translation Handler
	 * @param frontline 
	 */
	public NewTranslationHandler(UiGeneratorController ui, TranslationThinletTabController translationThinletTabController) {
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
	
	public void initDialog() {
		this.showFlagSelection();
	}

	/** @see UiGeneratorController#removeDialog(Object) */
	public void removeDialog() {
		this.ui.removeDialog(dialogComponent);
	}
	
	
	public void openBrowser(String url) {
		this.ui.openBrowser(url);
	}
	
	public void populate(MasterTranslationFile languageBundle) {
		Object tfLanguageName = find(COMPONENT_TF_LANGUAGE_NAME);
		Object tfIsoCode = find(COMPONENT_TF_ISO_CODE);
		
		this.ui.setText(tfLanguageName, languageBundle.getLanguageName());
		this.ui.setText(tfIsoCode, languageBundle.getLanguageCode());
		
		this.originalLanguageBundle = languageBundle;
	}

//> UI METHODS
	
	/**
	 * Create the new translation
	 * @param list
	 * @throws IOException 
	 */
	public void save(String languageName, String isoCode) throws IOException {
		if (this.isAllOkForNewTranslation(languageName, isoCode)) {
			if (isoCode.length() != FrontlineSMSConstants.ISO_639_1_CODE_LENGTH) {
				this.ui.alert(InternationalisationUtils.getI18NString(I18N_BAD_ISO_CODE, FrontlineSMSConstants.ISO_639_1_CODE_LENGTH));
			} else if (isLanguageTranslated(isoCode)) {
				this.ui.alert(InternationalisationUtils.getI18NString(I18N_LANGUAGE_ALREADY_TRANSLATED));
			} else {
				String countryCode = this.ui.getAttachedObject(this.ui.getSelectedItem(find(COMPONENT_COMBOBOX_COUNTRIES))).toString();
				
				// If this is a new language
				if (this.originalLanguageBundle == null) {
					this.owner.createNewLanguageFile(languageName, isoCode, countryCode);
				} else {
					this.owner.updateTranslationFile(this.originalLanguageBundle, languageName, isoCode, countryCode);
				}
				
				this.ui.infoMessage(InternationalisationUtils.getI18NString(I18N_TRANSLATION_SAVED, languageName));
				this.removeDialog();
			}
		}
	}

	/**
	 * Checks if the language is already translated
	 * @param isoCode ISO-639-1 Code of the language
	 * @return <code>true</code> if the language is already translated, <code>false</code> otherwise.
	 */
	public boolean isLanguageTranslated(String isoCode) {
		for(MasterTranslationFile languageBundle : MasterTranslationFile.getAll()) {
			// If the code is already present, we return true
			// But if the given ISO Code is the one that had been populated, it means that we are just editing this language
			if (languageBundle.getLanguageCode().equals(isoCode) && this.originalLanguageBundle != null && !isoCode.equals(this.originalLanguageBundle.getLanguageCode())) {
				return true;
			}
		}
		
		return false;
	}

//> INSTANCE HELPER METHODS
	/** Populate and display the countries in a Combo Box. */
	private void showFlagSelection() {
		Object countryList = find(COMPONENT_COMBOBOX_COUNTRIES);
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
	
	public void fieldChanged (String languageName, String isoCode) {
		if (isoCode != null && isoCode.length() > 2) {
			isoCode = isoCode.trim().substring(0, 2).toLowerCase();
			this.ui.setText(find(COMPONENT_TF_ISO_CODE), isoCode);
		}
		boolean enableDoneField = this.isAllOkForNewTranslation(languageName, isoCode);
		
		this.ui.setEnabled(find(COMPONENT_BT_SAVE), enableDoneField);
	}

	private boolean isAllOkForNewTranslation(String languageName, String isoCode) {
		return (languageName != null 
				&& isoCode != null
				&& find(COMPONENT_COMBOBOX_COUNTRIES) != null
				&& languageName.length() > 0
				&& isoCode.length() > 0
				//&& isoCode.length() == FrontlineSMSConstants.ISO_639_1_CODE_LENGTH
				&& this.ui.getSelectedIndex(find(COMPONENT_COMBOBOX_COUNTRIES)) >= 0);
	}

	private Object find (String component) {
		return this.ui.find(this.dialogComponent, component);
	}
}
