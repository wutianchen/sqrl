package ai.dataeng.sqml.io.impl.file;

import ai.dataeng.sqml.config.util.ConfigurationUtil;
import ai.dataeng.sqml.io.formats.FormatConfiguration;
import ai.dataeng.sqml.io.impl.CanonicalizerConfiguration;
import ai.dataeng.sqml.io.sources.DataSourceImplementation;
import ai.dataeng.sqml.io.sources.DataSourceConfiguration;
import ai.dataeng.sqml.io.sources.SourceTableConfiguration;
import ai.dataeng.sqml.io.sources.dataset.SourceDataset;
import ai.dataeng.sqml.io.sources.dataset.SourceTable;
import ai.dataeng.sqml.io.formats.FileFormat;
import ai.dataeng.sqml.tree.name.Name;
import ai.dataeng.sqml.tree.name.NameCanonicalizer;
import ai.dataeng.sqml.config.error.ErrorCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A {@link SourceDataset} that treats all files matching a certain set of extensions in a given directory as {@link SourceTable}.
 *
 */

@Slf4j
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DirectorySourceImplementation implements DataSourceImplementation, Serializable {

    public static final String DEFAULT_PATTERN = "_(\\d+)";

    @NonNull @NotNull @Size(min=3)
    String uri;
    @Builder.Default @NonNull @NotNull
    String partPattern = DEFAULT_PATTERN;
    @Builder.Default
    boolean discoverFiles = true;

    @JsonIgnore
    public FilePath getPath() {
        return new FilePath(uri);
    }

    @JsonIgnore
    private transient Pattern partPatternCompiled = null;

    @JsonIgnore
    public synchronized Pattern getPartPattern() {
        if (partPatternCompiled == null) {
            partPatternCompiled = Pattern.compile(this.partPattern +"$");
        }
        return partPatternCompiled;
    }

    @Override
    public boolean initialize(ErrorCollector errors) {
        FilePath directoryPath = new FilePath(uri);
        try {
            FilePath.Status status = directoryPath.getStatus();
            if (!status.exists() || !status.isDir()) {
                errors.fatal("URI [%s] is not a directory", uri);
                return false;
            }
        } catch (IOException e) {
            errors.fatal("URI [%s] is invalid: %s", uri, e);
            return false;
        }
        return true;
    }

    @Override
    @JsonIgnore
    public @NonNull Optional<String> getDefaultName() {
        //This method can be called prior to initialize, hence need to be defensive
        if (Strings.isNullOrEmpty(uri) || uri.length()<3) return Optional.empty();
        else return Optional.of(getPath().getFileName());
    }

    @Override
    public Collection<SourceTableConfiguration> discoverTables(DataSourceConfiguration config, ErrorCollector errors) {
        Map<Name, SourceTableConfiguration> tablesByName = new HashMap<>();
        gatherTables(getPath(),tablesByName,config.getNameCanonicalizer(),config.getFormat(),errors);
        return tablesByName.values();
    }

    private void gatherTables(FilePath directory, Map<Name, SourceTableConfiguration> tablesByName,
                              NameCanonicalizer canonicalizer, FormatConfiguration defaultFormat, ErrorCollector errors) {
        try {
            for (FilePath.Status fps : directory.listFiles()) {
                FilePath p = fps.getPath();
                if (fps.isDir()) {
                    gatherTables(p,tablesByName,canonicalizer,defaultFormat,errors);
                } else {
                    FilePath.NameComponents components = p.getComponents(getPartPattern());
                    if (Name.validName(components.getName())) {
                        FormatConfiguration format = defaultFormat;
                        FileFormat ff = FileFormat.getFormat(components.getFormat());
                        if (format == null && ff != null) {
                            format = ff.getImplementation().getDefaultConfiguration();
                        } else if (format == null) {
                            continue; //Unrecognized format
                        }
                        if (ff != null && !format.getFileFormat().equals(ff)) {
                            errors.warn("File [%s] does not match configured format [%s]",p,format.getFileFormat());
                            continue;
                        }
                        SourceTableConfiguration table = new SourceTableConfiguration(components.getName(),format);
                        Name tblName = canonicalizer.name(table.getName());
                        SourceTableConfiguration otherTbl = tablesByName.get(tblName);
                        if (otherTbl==null) tablesByName.put(tblName,table);
                        else if (!otherTbl.getFileFormat().equals(table.getFileFormat())) {
                            errors.warn("Table file [%s] does not have the same format [%s] of previously " +
                                    "encountered table [%s]. File will be ignored",
                                    p,otherTbl.getFileFormat(),otherTbl.getIdentifier());
                        }

                    }
                }
            }
        } catch (IOException e) {
            errors.fatal("Could not read directory [%s] during dataset refresh: %s",directory,e);
        }
    }

    public Collection<FilePath> getFilesForTable(SourceTableConfiguration tableConfig, DataSourceConfiguration config) throws IOException {
        List<FilePath> files = new ArrayList<>();
        gatherTableFiles(getPath(),files,tableConfig, config.getNameCanonicalizer());
        return files;
    }

    private void gatherTableFiles(FilePath directory, List<FilePath> files, SourceTableConfiguration tableConfig,
                                  NameCanonicalizer canonicalizer) throws IOException {
        for (FilePath.Status fps : directory.listFiles()) {
            FilePath p = fps.getPath();
            if (fps.isDir()) {
                gatherTableFiles(p,files,tableConfig, canonicalizer);
            } else if (isTableFile(p,tableConfig, canonicalizer)) {
                files.add(p);
            }
        }
    }

    public boolean isTableFile(FilePath file, SourceTableConfiguration tableConfig, NameCanonicalizer canonicalizer) {
        FilePath.NameComponents components = file.getComponents(getPartPattern());
        if (!canonicalizer.getCanonical(components.getName()).equals(tableConfig.getIdentifier())) return false;
        //If file has a format, it needs to match
        if (Strings.isNullOrEmpty(components.getFormat())) return true;
        else return tableConfig.getFileFormat().matches(components.getFormat());
    }

    @Override
    public boolean update(@NonNull DataSourceConfiguration config, @NonNull ErrorCollector errors) {
        errors.fatal("File data sources currently do not support updates");
        return false;
    }

}
