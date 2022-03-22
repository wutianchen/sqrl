package ai.dataeng.sqml.io.sources;

import ai.dataeng.sqml.config.ConfigurationError;
import ai.dataeng.sqml.tree.name.Name;
import ai.dataeng.sqml.tree.name.NameCanonicalizer;
import ai.dataeng.sqml.type.basic.ProcessMessage;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

public interface DataSource {

    /**
     * The name of the dataset produced by this data source.
     * The name must be unique within a server instance.
     *
     * @return name of dataset
     */
    @NonNull Name getDatasetName();

    @NonNull NameCanonicalizer getCanonicalizer();

    Collection<? extends SourceTableConfiguration> pollTables(long maxWait, TimeUnit timeUnit) throws InterruptedException;

    boolean isCompatible(@NonNull DataSource other, @NonNull ProcessMessage.ProcessBundle<ConfigurationError> errors);

    DataSourceConfiguration getConfiguration();

}
