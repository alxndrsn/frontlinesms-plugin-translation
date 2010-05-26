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
import net.frontlinesms.plugins.translation.TranslationThinletTabController;
import net.frontlinesms.resources.ResourceUtils;
import net.frontlinesms.ui.EnumCountry;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * This class is responsible for showing the first time wizard and handling its events.
 * 
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
@TextResourceKeyOwner(prefix={"MESSAGE_", "I18N"})
public class NewTranslationHandler implements ThinletUiEventHandler {
	
//> UI LAYOUT FILE PATHS
	/** [ui layout file path] The language selection page */
	private static final String UI_FILE_NEW_TRANSLATION = "/ui/plugins/translation/dgAddTranslation.xml";

	private static final String I18N_TRANSLATION_CREATED = "plugins.translation.translation.created";

//> UI COMPONENT NAMES
	private String COMPONENT_BT_SAVE = "btSave";
	private String COMPONENT_COMBOBOX_COUNTRIES = "cbCountries";
		
//> INSTANCE VARIABLES
	private Object dialogComponent;
	private UiGeneratorController ui;

	private TranslationThinletTabController owner;


//> CONSTRUCTORS
	/**
	 * New Translation Handler
	 * @param frontline 
	 */
	public NewTranslationHandler(UiGeneratorController ui, TranslationThinletTabController translationThinletTabController) {
		this.ui = ui;
		this.owner = translationThinletTabController;
	}
	
	/**
	 * @return the instance of the language selection dialog 
	 */
	public Object getDialog() {
		initDialog();
		
		return this.dialogComponent;
	}
	
	private void initDialog() {
		this.dialogComponent = this.ui.loadComponentFromFile(UI_FILE_NEW_TRANSLATION, this);
		this.showFlagSelection();
	}

	/** @see UiGeneratorController#removeDialog(Object) */
	public void removeDialog() {
		this.ui.removeDialog(dialogComponent);
	}
	
	
	public void openBrowser(String url) {
		this.ui.openBrowser(url);
	}

//> UI METHODS
	
	/**
	 * Create the new translation
	 * @param list
	 * @throws FileNotFoundException 
	 */
	public void createNewTranslation(String languageName, String isoCode) throws FileNotFoundException {
		if (this.isAllOkForNewTranslation(languageName, isoCode)) {
			String countryCode = this.ui.getAttachedObject(this.ui.getSelectedItem(find(COMPONENT_COMBOBOX_COUNTRIES))).toString();
			
			FileOutputStream fos = null;
			OutputStreamWriter osw = null;
			PrintWriter out = null;
			try {
				File file = new File(ResourceUtils.getConfigDirectoryPath() + "/languages/", "frontlineSMS_" + isoCode + ".properties");
				fos = new FileOutputStream(file);
				osw = new OutputStreamWriter(fos, InternationalisationUtils.CHARSET_UTF8);
				out = new PrintWriter(osw);
				out.write("# The 2-letter ISO-? code for the language\n" +
						"bundle.language=" + isoCode + "\n" +
						"# The name of the language IN THAT LANGUAGE - this is how the language will be chosen from\n" +
						"# menus, so it's important that speakers of that language actually understand this.  If the\n" +
						"# language name is in a non-Latin alphabet, a latinised or English version of the language\n" +
						"# name should also be provided, as some fonts will not display the non-Latin version\n" +
						"bundle.language.name=" + languageName + "\n" +
						"# 2-letter ISO-? code for the country where they speak this language.  This is used to get\n" +
						"# a flag to represent this language\n" +
						"bundle.language.country=" + countryCode + "\n" +
						"# Some alphabets may not be supported by the default font.  If this is the case, you can\n" +
						"# specify one or more font names here.  Each font name should be separated by a comma.\n" +
						"# You may need to tweak this on different systems.\n" +
						"#font.name=Courier New,Arial\n");
			} finally {
				if(out != null) out.close();
				if(osw != null) try { osw.close(); } catch(IOException ex) {}
				if(fos != null) try { fos.close(); } catch(IOException ex) {}
			}
			
			this.ui.infoMessage(InternationalisationUtils.getI18NString(I18N_TRANSLATION_CREATED, languageName));
			this.removeDialog();
			this.owner.refreshLanguageList();
		}
	}

//> INSTANCE HELPER METHODS
	/** Populate and display the countries in a Combo Box. */
	private void showFlagSelection() {
		Object countryList = find(COMPONENT_COMBOBOX_COUNTRIES);
		// Missing translation files
		for (EnumCountry enumCountry : EnumCountry.values()) {
			Object comboBoxChoice = this.ui.createComboboxChoice(enumCountry.getEnglishName(), enumCountry.getCode());
			this.ui.setIcon(comboBoxChoice, this.ui.getFlagIcon(enumCountry.getCode()));
			this.ui.add(countryList, comboBoxChoice);
		}
	}
	
	public void fieldChanged (String languageName, String isoCode) {
		boolean enableDoneField = this.isAllOkForNewTranslation(languageName, isoCode);
		
		this.ui.setEnabled(find(COMPONENT_BT_SAVE), enableDoneField);
	}

	private boolean isAllOkForNewTranslation(String languageName, String isoCode) {
		return (languageName != null 
				&& isoCode != null
				&& find(COMPONENT_COMBOBOX_COUNTRIES) != null
				&& languageName.length() > 0
				&& isoCode.length() == FrontlineSMSConstants.ISO_639_1_CODE_LENGTH
				&& this.ui.getSelectedIndex(find(COMPONENT_COMBOBOX_COUNTRIES)) > 0);
	}

	private Object find (String component) {
		return this.ui.find(this.dialogComponent, component);
	}
}
