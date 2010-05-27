/**
 * 
 */
package net.frontlinesms.plugins.translation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.Map.Entry;

import net.frontlinesms.plugins.BasePluginThinletTabController;
import net.frontlinesms.plugins.translation.ui.NewTranslationHandler;
import net.frontlinesms.resources.ResourceUtils;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.LanguageBundle;
import thinlet.Thinlet;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class TranslationThinletTabController extends BasePluginThinletTabController<TranslationPluginController> {

	//> STATIC CONSTANTS
	/** Filename and path of the XML for the Translation tab. */
	private final String UI_FILE_TRANSLATE_DIALOG = "/ui/plugins/translation/dgTranslate.xml";

	private final String COMPONENT_LS_LANGUAGES = "lsLanguages";

	private final String COMPONENT_TRANSLATION_CREATE = "newTranslation";
	private final String COMPONENT_TRANSLATION_EDIT = "editTranslation";

	private final String COMPONENT_PN_BIG_BUTTONS = "pnBigButtons";
	
	private final String UI_COMPONENT_PN_RESTART_FRONTLINE = "restartFrontline";
	private final String UI_COMPONENT_BT_SAVE = "saveTranslations";

//> INSTANCE VARIABLES
	/** Current view in the translations tables tabs */
	TranslationView visibleTab;
	/**
	 * List of all translation rows in each translation table.
	 * For each {@link TranslationView}, this map will contain all rows that could be shown, even those which are currently filtered out.
	 */
	private Map<TranslationView, List<Object>> translationTableRows;
	/** The dialog used for editing a translation.  When the dialog is not visible, this should be <code>null</code>. */
	private Object editDialog;
	/** The localized language file which we are currently editing/working on. */
	MasterTranslationFile selectedLanguageFile;

	/** The default {@link MasterTranslationFile}.  This is cached here to prevent it being reloaded regularly. */
	private MasterTranslationFile defaultLanguageBundle;

	private final String I18N_TRANSLATION_SAVED = "plugins.translation.translations.saved";

	private Map<String, MasterTranslationFile> languageBundles;

	//> CONSTRUCTORS
	protected TranslationThinletTabController(TranslationPluginController pluginController, UiGeneratorController uiController) {
		super(pluginController, uiController);
	}

	public void init() {
		this.visibleTab = TranslationView.ALL;
		this.defaultLanguageBundle =  MasterTranslationFile.getDefault();
		this.languageBundles = new HashMap<String, MasterTranslationFile>();
		refreshLanguageList();
	}

//> UI METHODS
	/** Method called when the current translation tab is changed. */
	public void tabChanged(int selectedTabIndex) {
		this.visibleTab = TranslationView.getFromTabIndex(selectedTabIndex);
	}

	/** UI Event method: show the editor for the selected translation in the translation table. */
	public void editText() {
		System.out.println("TranslationThinletTabController.editText()");
		String textKey = getSelectedTextKey(this.visibleTab);
		String defaultValue = "";
		try { defaultValue = getDefaultLanguageBundle().getValue(textKey); } catch(MissingResourceException ex) {};
		
		MasterTranslationFile selectedLanguageBundle = getSelectedLanguageBundle();
		String localValue = "";
		try { localValue = selectedLanguageBundle.getValue(textKey); } catch(MissingResourceException ex) {};
		
		// Load the dialog
		this.editDialog = ui.loadComponentFromFile(UI_FILE_TRANSLATE_DIALOG, this);
		ui.setText(ui.find(this.editDialog, "lbLocalTranslation"), selectedLanguageBundle.getLanguageName());
		
		// Initialize textfield values
		ui.setText(ui.find(this.editDialog, "tfKey"), textKey);
		ui.setText(ui.find(this.editDialog, "tfDefault"), defaultValue);
		ui.setText(ui.find(this.editDialog, "tfLocal"), localValue);
	
		// Display the dialog
		ui.add(this.editDialog);
	}

	/**
	 * UI Event method: triggered when the user tries to delete the translation on the list.
	 * The user is shown a dialog requesting that they confirm the action.
	 */
	public void confirmDeleteText() {
		String textKey = getSelectedTextKey(this.visibleTab);
		if(textKey != null) {
			ui.showConfirmationDialog("deleteText('" + textKey + "')", this);
		}
		
		removeEditDialog();
	}
	
	/**
	 * Method called when the user has confirmed his desire to delete a particular translation.
	 * @param textKey The key of the translation to delete.
	 * @throws IOException If there is a problem saving the translation file.
	 */
	public void deleteText(String textKey) throws IOException {
		MasterTranslationFile lang = this.getSelectedLanguageBundle();
		lang.delete(textKey);
		refreshTables();
		this.enableSaveButton(true);
		
		// save language bundle to disk
		//lang.saveToDisk(InternationalisationUtils.getLanguageDirectory());
		/*
		// Remove the deleted item from view and from the 
		Object selectedTableItem = getSelectedTableItem(this.visibleTab);
		ui.remove(selectedTableItem);
		for(List<Object> tableRows : this.translationTableRows.values()) {
			tableRows.remove(selectedTableItem);
		}
*/
		ui.removeConfirmationDialog();
	}
	
	/**
	 * Method called when a translation has been added or saved.  The translation is added to the relevant
	 * file, and the file is then saved.
	 * @param textKey
	 * @param textValue
	 * @throws IOException
	 */
	public void saveText(String textKey, String textValue) {
		MasterTranslationFile languageBundle = this.getSelectedLanguageBundle();
		languageBundle.add(textKey, textValue);
		
		if (!languageBundles.containsKey(languageBundle.getIdentifier())) {
			languageBundles.put(languageBundle.getIdentifier(), languageBundle);
		}
		
		// Update the table
		//setSelectedTextValue(this.visibleTab, textValue);
		
		int selectedIndex = this.ui.getSelectedIndex(getLanguageList());
		this.refreshLanguageList();
		if (selectedIndex >= 0) {
			this.ui.setSelectedIndex(getLanguageList(), selectedIndex);
			enableTranslationFields(this.ui.find(COMPONENT_PN_BIG_BUTTONS));
		}
		
		this.refreshTables();
		
		removeEditDialog();
		this.enableSaveButton(true);
	}
	
	public void saveTranslations () throws IOException {
		// save all language bundles to disk
		for (MasterTranslationFile mtf : languageBundles.values()) {
			mtf.saveToDisk(InternationalisationUtils.getLanguageDirectory());
		}
		
		this.enableSaveButton(false);
		this.ui.infoMessage(InternationalisationUtils.getI18NString(I18N_TRANSLATION_SAVED));
		this.ui.setVisible(this.ui.find(UI_COMPONENT_PN_RESTART_FRONTLINE), true);
	}
	
	private void enableSaveButton (boolean enable) {
		this.ui.setEnabled(this.ui.find(UI_COMPONENT_BT_SAVE), enable);
	}
	
	public void removeEditDialog() {
		ui.remove(this.editDialog);
		this.editDialog = null;
	}
	
	public void languageSelectionChanged() {
		System.out.println("TranslationThinletTabController.languageSelectionChanged()");
		refreshTables();
		enableTranslationFields(this.ui.find(COMPONENT_PN_BIG_BUTTONS));
		ui.setEnabled(getFilterTextfield(), true);
	}
	public void filterTranslations(String filterText) {
		System.out.println("TranslationThinletTabController.filterTranslations(" + filterText + ")");
		filterTable(TranslationView.ALL);
		filterTable(TranslationView.MISSING);
		filterTable(TranslationView.EXTRA);
	}
	
//>
	/**
	 * Filter the elements of a translation table, hiding all rows that do not match the filter text
	 * and showing all that do.
	 */
	private void filterTable(TranslationView view) {
		Object table = find(view.getTableName());
		String filterText = getFilterText();
		ui.removeAll(table);
		List<Object> tableRows = this.translationTableRows.get(view);
		if(tableRows != null) {
			for(Object tableRow : tableRows) {
				boolean show = rowMatches(tableRow, filterText);
				if(show) {
					ui.add(table, tableRow);
				}
			}
		}
	}

	/**
	 * Check if any column in the given table row matches our filter text.
	 * @param row Thinlet ROW element.
	 * @param filterText Text to filter with
	 * @return <code>true</code> if the filter text is contained in any column in the supplied row; <code>false</code> otherwise.
	 */
	private boolean rowMatches(Object row, String filterText) {
		assert(Thinlet.getClass(row).equals(Thinlet.ROW)) : "This method is only applicable to Thinlet <row/> components.";
		if(filterText.length() == 0) {
			// If the filter text is empty, there is no need to check - it will match everything!
			return true;
		}
		for(Object col : ui.getItems(row)) {
			if(ui.getText(col).contains(filterText)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return the text key selected in the table, or <code>null</code> if none is selected.
	 * @param table
	 * @return
	 */
	private String getSelectedTextKey(TranslationView view) {
		Object selectedItem = getSelectedTableItem(view);
		if(selectedItem == null) return null;
		String selectedKey = ui.getAttachedObject(selectedItem, String.class);
		return selectedKey;
	}

	private Object getSelectedTableItem(TranslationView view) {
		return ui.getSelectedItem(find(view.getTableName()));
	}
	
	private void setSelectedTextValue(TranslationView view, String value) {
		Object selectedItem = getSelectedTableItem(view);
		assert(selectedItem != null) : "Should not attempt to update the selected text item if none is selected";
		Object textColumn = ui.getItem(selectedItem, 2);
		ui.setText(textColumn, value);
	}
	
	private void refreshTables() {
		this.translationTableRows = new HashMap<TranslationView, List<Object>>();
		
		if (getSelectedLanguageBundle() == null) {
			ArrayList<Object> emptyList = new ArrayList<Object>();
			this.translationTableRows.put(TranslationView.ALL, emptyList);
			this.translationTableRows.put(TranslationView.MISSING, emptyList);
			this.translationTableRows.put(TranslationView.EXTRA, emptyList);
		} else {
			MasterTranslationFile lang = getSelectedLanguageBundle();
			MasterTranslationFile defaultLang = getDefaultLanguageBundle();
			Comparator<Object> comparator = new PropertyRowComparator(this.ui);  
			
			// Generate the "all" table rows
			ArrayList<Object> allRows = new ArrayList<Object>(defaultLang.getProperties().size());
			for(Entry<String, String> defaultEntry : defaultLang.getProperties().entrySet()) {
				String key = defaultEntry.getKey();
				String langValue = null;
				try {
					langValue = lang.getValue(key);
				} catch(MissingResourceException ex) {
					langValue = "";
				}
				Object tableRow = createTableRow(key, defaultEntry.getValue(), langValue);
				allRows.add(tableRow);
			}
			
			Collections.sort(allRows, comparator);
			this.translationTableRows.put(TranslationView.ALL, allRows);
			
			LanguageBundleComparison comp = new LanguageBundleComparison(defaultLang, lang);
			
			// populate and enable the missing table
			Set<String> missingKeys = comp.getKeysIn1Only();
			ArrayList<Object> missingRows = new ArrayList<Object>(missingKeys.size());
			for(String key : missingKeys) {
				Object tableRow = createTableRow(key, comp.get1(key), "");
				missingRows.add(tableRow);
			}
			
			Collections.sort(missingRows, comparator);
			this.translationTableRows.put(TranslationView.MISSING, missingRows);
	
			// populate and enable the extra table
			Set<String> extraKeys = comp.getKeysIn2Only();
			ArrayList<Object> extraRows = new ArrayList<Object>(extraKeys.size());
			for(String key : extraKeys) {
				Object tableRow = createTableRow(key, "", comp.get2(key));
				extraRows.add(tableRow);
			}
			
			Collections.sort(extraRows, comparator);
			this.translationTableRows.put(TranslationView.EXTRA, extraRows);
		}
		
		initTable(TranslationView.ALL);
		initTable(TranslationView.MISSING);
		initTable(TranslationView.EXTRA);
	}
	
	private void initTable(TranslationView view) {
		if (getSelectedLanguageBundle() != null) {
			Object table = find(view.getTableName());
			ui.setText(ui.find(table, "clCurrentLanguage"), getSelectedLanguageBundle().getLanguageName());
		}
		filterTable(view);
	}
	
	public void restartFrontlineSMS () {
		this.ui.reloadUi();
	}
	
	public void createTranslation () {
		NewTranslationHandler handler = new NewTranslationHandler(this.ui, this);
		handler.initDialog();
		this.ui.add(handler.getDialog());
	}
	
	public void editTranslation () {
		MasterTranslationFile languageBundle = this.getSelectedLanguageBundle();
		if (languageBundle != null) {
			String languageName = languageBundle.getLanguageName();
			String countryFlag = languageBundle.getCountry();
			String languageCode = languageBundle.getLanguageCode();
			
			NewTranslationHandler handler = new NewTranslationHandler(this.ui, this);
			
			handler.populate(languageBundle);
			handler.initDialog();
			this.ui.add(handler.getDialog());	
		}
	}
	
	public void deleteTranslation () {
		//NewTranslationHandler handler = new NewTranslationHandler(this.ui, this);
		//this.ui.add(handler.getDialog());
	}
	
	/**
	 * Enables or disables New/Edit/Delete translation buttons
	 */
	public void enableTranslationFields(Object component) {
		log.trace("ENTER");
		int selected = ui.getSelectedIndex(getLanguageList());
		if (selected <= 0) {
			log.debug("Nothing selected, so we only allow keyword creation.");
			for (Object o : ui.getItems(component)) {
				String name = ui.getString(o, Thinlet.NAME);
				if (name == null) {
					continue;
				} else {
					// "New" button is always enabled
					// "Edit" button is only enabled if a language different than the default language (English) is selected
					boolean isEnabled = (name.equals(COMPONENT_TRANSLATION_CREATE) || selected > 0);
					ui.setEnabled(o, isEnabled);
				}
			}
		} else {
			//Keyword selected
			for (Object o : ui.getItems(component)) {
				String name = ui.getString(o, Thinlet.NAME);
				boolean isEnabled =  (!name.equals(COMPONENT_TRANSLATION_EDIT) || !isSelectedItemEditing());
				ui.setEnabled(o, isEnabled);
			}
		}
		log.trace("EXIT");
	}
	
	private boolean isSelectedItemEditing() {
		if (this.ui.getSelectedItem(this.getLanguageList()) == null) {
			return false;
		}
		Object languageIdentifier = this.ui.getAttachedObject(this.ui.getSelectedItem(this.getLanguageList()));
		
		return languageBundles.containsKey(languageIdentifier);
	}

	/**
	 * Creates a Thinlet table row for a translation.  The first column value, i.e. 
	 * the translation key, is attached, to the row, as well as appearing in the first
	 * column.
	 * @param columnValues
	 * @return
	 */
	private Object createTableRow(String... columnValues) {
		assert(columnValues.length > 0) : "The translation key should be provided as the first column value.";
		Object row = ui.createTableRow(columnValues[0]);
		for(int i = 0 ; i < columnValues.length ; ++i) {
			String col = columnValues[i];
			ui.add(row, ui.createTableCell(col));
		}
		return row;
	}

//> INSTANCE HELPER METHODS
	/** Refresh UI elements */
	public void refreshLanguageList() {
		// Refresh language list
		Object languageList = getLanguageList();
		super.removeAll(languageList);
		System.err.println(languageBundles);
		for (MasterTranslationFile languageBundle : MasterTranslationFile.getAll()) {
			boolean shouldBeBold = languageBundles.containsKey(languageBundle.getIdentifier());
			Object item = ui.createListItem(languageBundle.getLanguageName(), languageBundle.getIdentifier(), shouldBeBold);
			ui.setIcon(item, ui.getFlagIcon(languageBundle));
			ui.add(languageList, item);
		}
		
		this.enableTranslationFields(find(COMPONENT_PN_BIG_BUTTONS));
	}
	
//> UI ACCESSORS
	private Object getLanguageList() {
		return super.find(COMPONENT_LS_LANGUAGES);
	}
	
	private synchronized MasterTranslationFile getSelectedLanguageBundle() {
		if (ui.getSelectedItem(getLanguageList()) == null) {
			return null;
		}
		
		String languageFileIdentifier = ui.getAttachedObject(ui.getSelectedItem(getLanguageList()), String.class);
		if (languageBundles.containsKey(languageFileIdentifier)) {
			return languageBundles.get(languageFileIdentifier);
		} else {
			return MasterTranslationFile.getFromIdentifier(languageFileIdentifier);
		}
	}
	
	private MasterTranslationFile getDefaultLanguageBundle() {
		return this.defaultLanguageBundle;
	}
	
	public LanguageBundle getCurrentResourceBundle () {
		return this.ui.currentResourceBundle;
	}

	private String getFilterText() {
		return ui.getText(getFilterTextfield());
	}

	private Object getFilterTextfield() {
		return find("tfTranslationFilter");
	}
	
	/**
	 * Classe used to sort the properties alphabetically in the lists
	 * @author Morgan Belkadi <morgan@frontlinesms.com>
	 */
	public class PropertyRowComparator implements Comparator<Object> {
		/** Instance of {@link UiGeneratorController} used to get the attached objects */
		private UiGeneratorController ui;

		public PropertyRowComparator (UiGeneratorController ui) {
			this.ui = ui;
		}
		
		/**
		 * Inherited compare method from Comparator interface
		 * Comparison is made on the attached object (String) 
		 */
		public int compare(Object o1, Object o2) {
			return ((Comparable)this.ui.getAttachedObject(o1)).compareTo(this.ui.getAttachedObject(o2));
		}
	}

	public UiGeneratorController getUIGeneratorController() {
		return this.ui;
	}

	public void updateTranslationFile(MasterTranslationFile originalLanguageBundle, String languageName, String isoCode, String countryCode) throws IOException {
		MasterTranslationFile newLanguageBundle = new MasterTranslationFile(originalLanguageBundle.getFilename(), originalLanguageBundle.getTranslationFiles());
		
		newLanguageBundle.setCountry(countryCode);
		newLanguageBundle.setLanguageName(languageName);
		newLanguageBundle.setLanguageCode(isoCode);
		newLanguageBundle.saveToDisk(new File(ResourceUtils.getConfigDirectoryPath() + "/languages/"));
		
		if (!isoCode.equals(originalLanguageBundle.getLanguageCode())) {
			
			// If the ISO code has changed during the editing, we have to rename the file
			File oldFile = new File(ResourceUtils.getConfigDirectoryPath() + "/languages/", originalLanguageBundle.getFilename());
			String newFilename = "frontlineSMS_" + isoCode + ".properties";
			File newFile = new File(ResourceUtils.getConfigDirectoryPath() + "/languages/", newFilename);
			newLanguageBundle.setFilename(newFilename);
			oldFile.renameTo(newFile);
		}
		
		// If the Bundle was editing (shouldn't happen), we put the updated version in the map
		if (this.languageBundles.remove(originalLanguageBundle.getIdentifier()) != null) {
			this.languageBundles.put(newLanguageBundle.getIdentifier(), newLanguageBundle);
		}
	}
}

enum TranslationView {
	ALL("tbAll", 0, "tbAllTranslations"),
	MISSING("tbMissing", 1, "tbMissingTranslations"),
	EXTRA("tbExtra", 2, "tbExtraTranslations");
	
	private final String tabName;
	private final int tabIndex;
	private final String tableName;
	
//> CONSTRUCTOR
	private TranslationView(String tabName, int tabIndex, String tableName) {
		this.tabName = tabName;
		this.tabIndex = tabIndex;
		this.tableName = tableName;
	}

//> ACCESSORS
	public String getTabName() {
		return tabName;
	}

	public int getTabIndex() {
		return tabIndex;
	}

	public String getTableName() {
		return tableName;
	}
	
//> STATIC METHODS
	static TranslationView getFromTabIndex(int tabIndex) {
		for(TranslationView view : TranslationView.values()) {
			if(view.getTabIndex() == tabIndex) {
				return view;
			}
		}
		// No match was found
		return null;
	}
}
