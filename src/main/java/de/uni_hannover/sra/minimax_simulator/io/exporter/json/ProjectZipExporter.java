package de.uni_hannover.sra.minimax_simulator.io.exporter.json;

import de.uni_hannover.sra.minimax_simulator.io.IOUtils;
import de.uni_hannover.sra.minimax_simulator.io.exporter.ProjectExportException;
import de.uni_hannover.sra.minimax_simulator.io.exporter.ProjectExporter;
import de.uni_hannover.sra.minimax_simulator.io.importer.json.ProjectZipImporter;
import de.uni_hannover.sra.minimax_simulator.model.user.Project;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link ProjectExporter} that writes a project to a {@link File}. <br>
 * <br>
 * After a successful export, the written file will be a zip file containing several definitions in
 * form of JSON files. <br>
 * <br>
 * The files produced by this class can be parsed by a {@link ProjectZipImporter} again, however
 * this only is guaranteed for files that are parsed by an importer that is of the same application
 * version as this exporter.
 * 
 * @author Martin L&uuml;ck
 */
public class ProjectZipExporter implements ProjectExporter {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final File file;

    /**
     * Prepares a new exporter that will write an exported project as a zip archive to the given
     * file.
     *
     * @param file
     *            the (non-null) file that will be (over-)written
     */
    public ProjectZipExporter(File file) {
        this.file = checkNotNull(file, "Invalid Null argument: file");
    }

    @Override
    public void exportProject(Project project) throws ProjectExportException {
        checkNotNull(project);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);

            ZipOutputStream zos = new ZipOutputStream(fos);
            writeZipEntries(zos, project);
        } catch (FileNotFoundException e) {
            throw new ProjectExportException("Target file for export of project cannot be opened: " + file.getPath(), e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    /**
     * Actually writes the specified {@link Project} to disk. The method creates an own {@code ZipEntry} for the
     * {@code MachineConfiguration} and {@code SignalTable}.
     *
     * @param zos
     *          the {@code ZipOutputStream} used for writing
     * @param project
     *          the {@code Project} to export
     * @throws ProjectExportException
     *          thrown if the project could not be saved
     */
    private void writeZipEntries(ZipOutputStream zos, Project project) throws ProjectExportException {
        try {
            ZipEntry machineFile = new ZipEntry("machine.json");
            zos.putNextEntry(machineFile);
            new MachineJsonExporter().write(new OutputStreamWriter(zos, CHARSET), project.getMachineConfiguration());
            zos.closeEntry();

            ZipEntry signalTableEntry = new ZipEntry("signal.json");
            zos.putNextEntry(signalTableEntry);
            new SignalJsonExporter().write(new OutputStreamWriter(zos, CHARSET), project.getSignalTable());
            zos.closeEntry();
        } catch (IOException ioe) {
            throw new ProjectExportException("I/O Error while exporting project into file: " + file.getPath(), ioe);
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }
}