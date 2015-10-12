package de.uni_hannover.sra.minimax_simulator.gui;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.uni_hannover.sra.minimax_simulator.Main;
import de.uni_hannover.sra.minimax_simulator.io.ProjectImportException;
import de.uni_hannover.sra.minimax_simulator.io.exporter.csv.SignalCsvExporter;
import de.uni_hannover.sra.minimax_simulator.io.exporter.csv.SignalHtmlExporter;
import de.uni_hannover.sra.minimax_simulator.model.machine.base.display.MachineDisplayListener;
import de.uni_hannover.sra.minimax_simulator.model.signal.SignalConfiguration;
import de.uni_hannover.sra.minimax_simulator.model.signal.SignalTable;
import de.uni_hannover.sra.minimax_simulator.model.user.Project;
import de.uni_hannover.sra.minimax_simulator.model.user.Workspace;
import de.uni_hannover.sra.minimax_simulator.model.user.WorkspaceListener;
import de.uni_hannover.sra.minimax_simulator.resources.TextResource;
import de.uni_hannover.sra.minimax_simulator.ui.UI;
import de.uni_hannover.sra.minimax_simulator.ui.UIUtil;
import de.uni_hannover.sra.minimax_simulator.ui.common.dialogs.*;
import de.uni_hannover.sra.minimax_simulator.ui.schematics.MachineSchematics;
import de.uni_hannover.sra.minimax_simulator.ui.schematics.SpriteOwner;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * <b>The main controller for the JavaFX GUI.</b><br>
 * <br>
 * This controller handles the GUI interactions with the {@link MenuBar} and {@link TabPane}
 * as well as the bound shortcuts.
 *
 * @author Philipp Rohde
 */
public class FXMainController implements WorkspaceListener, MachineDisplayListener {

    @FXML private Menu menuProject;
    @FXML private MenuItem project_new;
    @FXML private MenuItem project_open;
    @FXML private MenuItem project_save;
    @FXML private MenuItem project_saveas;
    @FXML private MenuItem project_export_schematics;
    @FXML private MenuItem project_export_signal;
    @FXML private MenuItem project_close;
    @FXML private MenuItem exitApplication;
    private List<MenuItem> disabledMenuItems = null;

    @FXML private Menu menuView;
    @FXML private MenuItem view_overview;
    @FXML private MenuItem view_memory;
    @FXML private MenuItem view_debugger;

    @FXML private Menu menuMachineConfiguration;
    @FXML private MenuItem view_conf_alu;
    @FXML private MenuItem view_conf_reg;
    @FXML private MenuItem view_conf_mux;
    @FXML private MenuItem view_conf_signal;

    @FXML private Menu menuHelp;
    @FXML private MenuItem help_about;

    @FXML private TabPane tabpane;
    @FXML private Tab tab_overview;
    @FXML private Tab tab_signal;
    @FXML private Tab tab_memory;
    @FXML private Tab tab_debugger;
    @FXML private Tab tab_alu;
    @FXML private Tab tab_reg;
    @FXML private Tab tab_mux;

    @FXML private ScrollPane paneOverview;

    private FileChooser fc = new FileChooser();

    @FXML private MemoryView embeddedMemoryViewController;
    @FXML private AluView embeddedAluViewController;
    @FXML private MuxView embeddedMuxViewController;
    @FXML private RegView embeddedRegViewController;
    @FXML private DebuggerView embeddedDebuggerViewController;
    @FXML private SignalView embeddedSignalViewController;

    private MachineSchematics schematics;

    private final TextResource _res;

    private Map<String, Tab> tabMap;

    private static final String _versionString = Main.getVersionString();
    private static final Workspace _ws = Main.getWorkspace();

    private final ExtensionFilter extFilterSignal;
    private final ExtensionFilter extFilterProject;
    private final ExtensionFilter extFilterSchematics;

    /**
     * The constructor initializes the final variables.
     */
    public FXMainController() {
        _res = Main.getTextResource("application");

        extFilterSignal = new ExtensionFilter(_res.get("project.signalfile.description"), "*.csv", "*.html");
        extFilterProject = new ExtensionFilter(_res.get("project.filedescription"), "*.zip");
        extFilterSchematics = new ExtensionFilter(_res.get("project.imagefile.description"), "*.jpg", "*.png");
    }

    /**
     * This method is called during application start up and initializes the GUI.
     */
    public void initialize() {
        _ws.addListener(this);

        this.tabMap = ImmutableMap.<String, Tab>builder()
                .put("view_project_overview",   tab_overview)
                .put("view_machine_alu", tab_alu)
                .put("view_machine_register",   tab_reg)
                .put("view_machine_mux",        tab_mux)
                .put("view_machine_signal",     tab_signal)
                .put("view_project_memory", tab_memory)
                .put("view_project_debugger", tab_debugger)
                .build();

        this.disabledMenuItems = ImmutableList.<MenuItem>builder()
                .add(project_saveas, project_export_schematics, project_export_signal, project_close, view_overview, view_memory, view_debugger)
                .build();

        setShortcuts();
        setLocalizedTexts();
    }

    /**
     * Binds the shortcuts to the {@link MenuItem}s.
     */
    private void setShortcuts() {
        // menu: project
        this.project_new.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        this.project_open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        this.project_save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        this.project_close.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        this.exitApplication.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));

        // menu: view
        this.view_overview.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
        this.view_memory.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN));
        this.view_debugger.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN));

        // menu: machine configuration
        this.view_conf_alu.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        this.view_conf_reg.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        this.view_conf_mux.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN));
        this.view_conf_signal.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN));
    }

    /**
     * Sets localized texts from resource for the GUI elements.
     */
    private void setLocalizedTexts() {
        TextResource res = Main.getTextResource("menu");
        // menu: project
        menuProject.setText(res.get("project"));
        final List<MenuItem> projectMenu = new ArrayList<>(Arrays.asList(project_new, project_open, project_save, project_saveas, project_export_schematics, project_export_signal, project_close, exitApplication,
                view_overview, view_memory, view_debugger, view_conf_alu, view_conf_mux, view_conf_reg, view_conf_signal, help_about));
        for (MenuItem mi : projectMenu) {
            String id = mi.getId().replace("_", ".");
            String mne = res.get(id + ".mne");

            String text = res.get(id);
            if (!mne.isEmpty()) {
                text = text.replaceFirst(mne, "_"+mne);
                mi.setMnemonicParsing(true);
            }
            mi.setText(text);
        }

        final List<Menu> allMenus = new ArrayList<>(Arrays.asList(menuProject, menuView, menuHelp));
        for (Menu m : allMenus) {
            String id = m.getId().replace("_", ".");
            String mne = res.get(id + ".mne");

            String text = res.get(id);
            if (!mne.isEmpty()) {
                text = text.replaceFirst(mne, "_"+mne);
                m.setMnemonicParsing(true);
            }
            m.setText(text);
        }

        // menu: help
        menuHelp.setText(res.get("help"));

        // tabs
        res = Main.getTextResource("project");
        final List<Tab> tabsProject = new ArrayList<>(Arrays.asList(tab_debugger, tab_memory, tab_overview));
        for (Tab tab : tabsProject) {
            String id = tab.getId();
            tab.setText(res.get(id.replace("_", ".")+".title"));
        }
        res = Main.getTextResource("machine");
        final List<Tab> tabsMachine = new ArrayList<>(Arrays.asList(tab_alu, tab_mux, tab_reg, tab_signal));
        for (Tab tab : tabsMachine) {
            String id = tab.getId();
            tab.setText(res.get(id.replace("_", ".") + ".title"));
        }
    }

    /**
     * Shuts down the application.
     *
     * @return
     *          {@code true} if the application will be shut down; {@code false} otherwise
     */
    public boolean exitApplication() {
        if (confirmDismissUnsavedChanges(_res.get("close-project.exit.title"), _res.get("close-project.exit.message"))) {
            Platform.exit();
            return true;
        }
        return false;
    }

    /**
     *
     * @param dialogTitle
     *          the title of the {@link FXUnsavedDialog}
     * @param dialogMessage
     *          the message of the {@link FXUnsavedDialog}
     * @return
     *          {@code true} if changes will be dismissed or were saved; {@code false} otherwise
     */
    // TODO: move to UIUtil?
    private boolean confirmDismissUnsavedChanges(String dialogTitle, String dialogMessage) {
        if (Main.getWorkspace().isUnsaved()) {
            ButtonType choice = new FXUnsavedDialog(dialogTitle, dialogMessage).getChoice();
            if (choice.equals(ButtonType.YES)) {
                if (!saveConfirmed()) {
                    return false;
                }
            }
            else if (choice.equals(ButtonType.CANCEL)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calls {@link #saveProjectAs} if the project was not saved yet, calls {@link #saveProject()} otherwise.
     *
     * @return
     *          {@code true} if the project was saved; {@code false} otherwise
     */
    private boolean saveConfirmed() {
        if (Main.getWorkspace().getCurrentProjectFile() == null) {
            return saveProjectAs();
        }
        else {
            return saveProject();
        }
    }

    /**
     * Creates a new project with default project data and initializes the rest of the GUI.
     */
    public void newProject() {
        if (!confirmDismissUnsavedChanges(_res.get("close-project.generic.title"), _res.get("close-project.generic.message"))) {
            return;
        }

        UIUtil.executeWorker(new Runnable() {
            @Override
            public void run() {
                try {
                    Main.getWorkspace().newProject();
                    initProjectGUI();
                } catch (RuntimeException e) {
                    closeProject();
                    throw e;
                }
            }
        }, _res.get("wait.title"), _res.get("wait.project.new"));
    }

    /**
     * Opens a new project from file.
     */
    public void openProject() {
        if (!confirmDismissUnsavedChanges(_res.get("close-project.generic.title"), _res.get("close-project.generic.message"))) {
            return;
        }

        fc.getExtensionFilters().clear();
        fc.getExtensionFilters().add(extFilterProject);

        File lastFolder = Main.getWorkspace().getLastProjectFolder();
        if (lastFolder != null && lastFolder.exists()) {
            fc.setInitialDirectory(lastFolder);
        }

        File file = fc.showOpenDialog(Main.getPrimaryStage());

        if (file == null) {
            return;
        }

        UIUtil.executeWorker(new Runnable() {
            @Override
            public void run() {
                try {
                    Main.getWorkspace().openProject(file);
                    //TODO: do all views work correctly?
                    initProjectGUI();
                } catch (ProjectImportException e) {
                    closeProject();
                    UI.invokeInFAT(new Runnable() {
                        @Override
                        public void run() {
                            new FXDialog(Alert.AlertType.ERROR, _res.get("load-error.title"), _res.get("load-error.message")).showAndWait();
                        }
                    });
                    //log.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }, _res.get("wait.title"), _res.format("wait.project.load", file.getName()));

    }

    /**
     * Prepares the GUI for working with a project.
     */
    private void initProjectGUI() {
        UI.invokeInFAT(new Runnable() {
            @Override
            public void run() {
                setDisable(false);
                initTabs();
                closeNonDefaultTabs();
            }
        });
    }

    /**
     * Initializes all tabs of the simulator.
     */
    private void initTabs() {
        embeddedMemoryViewController.initMemoryView();
        embeddedAluViewController.initAluView();
        embeddedMuxViewController.initMuxView();
        embeddedRegViewController.initRegView();
        embeddedDebuggerViewController.initDebuggerView();
        embeddedSignalViewController.initSignalView();

        // init overview tab
        this.schematics = new MachineSchematics(Main.getWorkspace().getProject().getMachine());
        //this.schematics.translateXProperty().bind(paneOverview.widthProperty().subtract(schematics.widthProperty()).divide(2));
        //paneOverview.setContent(this.schematics);

        // the canvas didn't resize itself correctly on mac; here is the workaround
        schematicsToImage();
    }

    /**
     * Workaround for Mac:
     * Using the schematics itself for rendering caused a bug on Mac. The canvas did resize but didn't fill
     * the new occupied space so the schematics were cropped. Using an image instead of the canvas solved the problem.
     */
    private void schematicsToImage() {
        ImageView imgView = new ImageView();
        Image img = this.schematics.snapshot(null, null);

        imgView.translateXProperty().bind(paneOverview.widthProperty().subtract(img.widthProperty()).divide(2));
        imgView.setFitHeight(img.getHeight());
        imgView.setFitWidth(img.getWidth());

        imgView.setImage(img);
        paneOverview.setContent(imgView);
    }

    /**
     * Removes the non-default {@link Tab}s from the {@link TabPane}
     */
    private void closeNonDefaultTabs() {
        tabpane.getTabs().removeAll(tab_alu, tab_reg, tab_mux);
    }

    /**
     * Saves the current project to the current file.
     *
     * @return
     *          {@code true} if the project was saved; {@code false} otherwise
     */
    public boolean saveProject() {
        return saveProjectToFile(Main.getWorkspace().getCurrentProjectFile());
    }

    /**
     * Saves the current project to another file.
     *
     * @return
     *          {@code true} if the project was saved; {@code false} otherwise
     */
    public boolean saveProjectAs() {
        fc.getExtensionFilters().clear();
        fc.getExtensionFilters().add(extFilterProject);
        File file = fc.showSaveDialog(Main.getPrimaryStage());
        return saveProjectToFile(file);
    }

    /**
     * Saves the current project to the given file.
     *
     * @param file
     *          the {@code File} the project should be saved to
     * @return
     *          {@code true} if the project was saved; {@code false} otherwise
     */
    private boolean saveProjectToFile(File file) {
        if (file == null) {
            return false;
        }

        if (file.getName().lastIndexOf('.') == -1) {
            // append ending
            file = new File(file.getPath() + ".zip");
        }

        final File fileToSave = file;

        UIUtil.executeWorker(new Runnable() {
            @Override
            public void run() {
                try {
                    Main.getWorkspace().saveProject(fileToSave);
                } catch (Exception e) {
                    Main.getWorkspace().closeProject();		// TODO: really closing project if saving didn't work?
                    throw Throwables.propagate(e);
                }
            }
        }, _res.get("wait.title"), _res.format("wait.project.save", file.getName()));
        return true;
    }

    /**
     * Exports the schematics of the current project.
     */
    public void exportSchematics() {
        fc.getExtensionFilters().clear();
        fc.getExtensionFilters().add(extFilterSchematics);
        File file = fc.showSaveDialog(Main.getPrimaryStage());

        exportSchematicsToFile(file);
    }

    /**
     * Saves the schematics of the current project to the given file.
     *
     * @param file
     *          the {@code File} the {@code MachineSchematics} should be saved to
     */
    private void exportSchematicsToFile(File file) {
        if (file == null) {
            return;
        }

        if (file.getName().lastIndexOf(".") == -1) {
            file = new File(file.getPath() + ".png");
        }

        final File imageFile = file;

        int dot = imageFile.getName().lastIndexOf('.');
        final String ending = imageFile.getName().substring(dot + 1);

        // get an image of the schematics
        final WritableImage image = this.schematics.snapshot(null, null);

        UIUtil.executeWorker(new Runnable() {
            @Override
            public void run() {
                // write the image to disk
                try {
                    // TODO: pure JavaFX
                    if (!ImageIO.write(SwingFXUtils.fromFXImage(image, null), ending, imageFile)) {
                        ioError(imageFile.getPath(), _res.get("project.export.error.message.ioex"));
                        return;
                    }
                } catch (IOException e1) {
                    // (almost) ignore
                    e1.printStackTrace();
                    ioError(imageFile.getPath(), _res.get("project.export.error.message.ioex"));
                    return;
                }
                // open the image
                try {
                    Main.getHostServicesStatic().showDocument(imageFile.getAbsolutePath());
                } catch (Exception e) {
                    // (almost) ignore
                    e.printStackTrace();
                }
            }
        }, _res.get("wait.title"), _res.get("wait.image-export"));
    }

    /**
     * Exports the {@link de.uni_hannover.sra.minimax_simulator.model.signal.SignalTable} of the current project.
     */
    public void exportSignal() {
        fc.getExtensionFilters().clear();
        fc.getExtensionFilters().add(extFilterSignal);
        File file = fc.showSaveDialog(Main.getPrimaryStage());

        exportSignalToFile(file);
    }

    /**
     * Saves the {@code SignalTable} to the given file.
     *
     * @param file
     *          the {@code File} the {@code SignalTable} should be saved to
     */
    private void exportSignalToFile(File file) {
        final Project project = Main.getWorkspace().getProject();

        if (file == null) {
            return;
        }

        if (file.getName().lastIndexOf(".") == -1) {
            // append ending
            file = new File(file.getPath() + ".html");
        }

        final File fileToSave = file;

        UIUtil.executeWorker(new Runnable() {
            @Override
            public void run() {
                try {
                    SignalTable table = project.getSignalTable();
                    SignalConfiguration config = project.getSignalConfiguration();
                    if (fileToSave.getName().endsWith(".csv")) {
                        new SignalCsvExporter(fileToSave).exportSignalTable(table, config);
                    }
                    else if (fileToSave.getName().endsWith(".html")) {
                        new SignalHtmlExporter(fileToSave).exportSignalTable(table, config);
                    }
                    else {
                        ioError(fileToSave.getPath(), _res.get("project.export.error.message.wrongformat"));
                    }
                } catch (IOException e1) {
                    // (almost) ignore
                    e1.printStackTrace();

                    ioError(fileToSave.getPath(), _res.get("project.export.error.message.ioex"));
                }
            }

        }, _res.get("wait.title"), _res.get("wait.signal-export"));
    }

    /**
     * Opens an error dialog if an {@code IOException} was thrown during export of the {@code MachineSchematics}
     * or {@code SignalTable}.
     *
     * @param filename
     *          the filename of the {@code File} where something went wrong
     * @param reason
     *          the reason of the error
     */
    private void ioError(String filename, String reason) {
        String error = _res.format("project.export.error.message", filename, reason);
        String title = _res.get("project.export.error.title");

        UI.invokeInFAT(new Runnable() {
            @Override
            public void run() {
                new FXDialog(Alert.AlertType.ERROR, title, error).showAndWait();
            }
        });
    }

    /**
     * Closes the current project.
     */
    public void closeProject() {
        if (!confirmDismissUnsavedChanges(_res.get("close-project.generic.title"), _res.get("close-project.generic.message"))) {
            return;
        }

        setDisable(true);
        Main.getWorkspace().closeProject();
    }

    /**
     * Disables/Enables the project dependent {@link MenuItem}s.
     * This method is called if a project was opened or closed.
     *
     * @param disabled
     *          whether the GUI components should be disabled
     */
    private void setDisable(boolean disabled) {
        for (MenuItem mi : disabledMenuItems) {
            mi.setDisable(disabled);
        }

        menuMachineConfiguration.setDisable(disabled);

        boolean visible = !disabled;
        tabpane.setVisible(visible);

    }

    /**
     * Opens the {@link Tab} corresponding to the {@link ActionEvent} calling the method.
     *
     * @param ae
     *          the {@link ActionEvent} calling the method
     */
    public void openTab(ActionEvent ae) {

        if (!(ae.getSource() instanceof MenuItem)) {
            return;
        }

        MenuItem caller = (MenuItem) ae.getSource();
        String id = caller.getId();

        Tab toAdd = tabMap.get(id);

        if (toAdd == null) {
            return;
        }

        if (!tabpane.getTabs().contains(toAdd)) {
            if (toAdd.equals(tab_overview)) {
                tabpane.getTabs().add(0, tab_overview);
            }
            else {
                tabpane.getTabs().add(toAdd);
            }
        }
        tabpane.getSelectionModel().select(toAdd);
    }

    /**
     * Shows the about dialog.
     */
    public void openInfo() {
        new FXAboutDialog().showAndWait();
    }

    /**
     * Gets the name of the currently open {@link Project}.
     *
     * @return
     *          the name of the currently open {@link Project}
     */
    private String getProjectName() {
        File file = Main.getWorkspace().getCurrentProjectFile();
        if (file == null) {
            return _res.get("project.unnamed");
        }
        return file.getName();
    }

    /**
     * Sets the title of the application.
     *
     * @param newTitle
     *              the title to set
     */
    private void setApplicationTitle(String newTitle) {
        UI.invokeInFAT(new Runnable() {
            @Override
            public void run() {
                Main.getPrimaryStage().setTitle(newTitle);
            }
        });
    }

    /**
     * Sets the application title with the name of the opened project.
     *
     * @param project
     *          the opened {@link Project}
     */
    @Override
    public void onProjectOpened(Project project) {
        setApplicationTitle(_res.format("title.open-project", _versionString, getProjectName()));
    }

    /**
     * Sets the application title with the name of the saved project.
     *
     * @param project
     *          the saved {@link Project}
     */
    @Override
    public void onProjectSaved(Project project) {
        setApplicationTitle(_res.format("title.open-project", _versionString, getProjectName()));
        project_save.setDisable(true);
    }

    /**
     * Sets the application title without any project name.
     *
     * @param project
     *          the closed {@link Project}
     */
    @Override
    public void onProjectClosed(Project project) {
        setApplicationTitle(_res.format("title", _versionString));
    }

    /**
     * Sets the application title with the modified project and marks it as unsaved.
     *
     * @param project
     *          the modified {@link Project}
     */
    @Override
    public void onProjectDirty(Project project) {
        setApplicationTitle(_res.format("title.open-unsaved-project", _versionString, getProjectName()));
        if (Main.getWorkspace().getCurrentProjectFile() != null) {
            project_save.setDisable(false);
        }
    }

    @Override
    public void machineSizeChanged() {
        schematicsToImage();
    }

    @Override
    public void machineDisplayChanged() {
        schematicsToImage();
    }

    @Override
    public void onSpriteOwnerAdded(SpriteOwner spriteOwner) {
        // not needed here
    }

    @Override
    public void onSpriteOwnerRemoved(SpriteOwner spriteOwner) {
        // not needed here
    }

    @Override
    public void onSpriteOwnerChanged(SpriteOwner spriteOwner) {
        // not needed here
    }
}
