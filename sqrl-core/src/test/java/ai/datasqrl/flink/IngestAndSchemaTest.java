package ai.datasqrl.flink;

import ai.datasqrl.C360Example;
import ai.datasqrl.api.ConfigurationTest;
import ai.datasqrl.environment.Environment;
import ai.datasqrl.config.SqrlSettings;
import ai.datasqrl.execute.flink.environment.FlinkStreamEngine;
import ai.datasqrl.execute.flink.environment.LocalFlinkStreamEngineImpl;
import ai.datasqrl.execute.flink.ingest.DataStreamProvider;
import ai.datasqrl.execute.flink.ingest.schema.SchemaValidationProcess;
import ai.datasqrl.execute.flink.ingest.schema.FlinkInputHandler;
import ai.datasqrl.execute.flink.ingest.schema.FlinkInputHandlerProvider;
import ai.datasqrl.io.impl.file.DirectorySourceImplementation;
import ai.datasqrl.io.sources.SourceRecord;
import ai.datasqrl.io.sources.dataset.DatasetRegistry;
import ai.datasqrl.io.sources.dataset.SourceDataset;
import ai.datasqrl.io.sources.dataset.SourceTable;
import ai.datasqrl.io.sources.stats.SourceTableStatistics;
import ai.datasqrl.parse.tree.name.Name;
import ai.datasqrl.environment.ImportManager;
import ai.datasqrl.config.error.ErrorCollector;
import ai.datasqrl.schema.input.SchemaAdjustmentSettings;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.bridge.java.internal.StreamTableEnvironmentImpl;
import org.apache.flink.types.Row;
import org.apache.flink.util.OutputTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.apache.flink.table.api.Expressions.call;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IngestAndSchemaTest {

  Environment env = null;
  DatasetRegistry registry = null;
  FlinkInputHandlerProvider inputHandlerProvider = new FlinkInputHandlerProvider();

  @BeforeEach
  public void setup() throws IOException {
      if (FileUtils.isDirectory(ConfigurationTest.dbPath.toFile()))
        FileUtils.cleanDirectory(ConfigurationTest.dbPath.toFile());
      SqrlSettings settings = ConfigurationTest.getDefaultSettings(true);
      env = Environment.create(settings);
      registry = env.getDatasetRegistry();
  }

  @AfterEach
  public void close() {
      env.close();
      env = null;
      registry = null;
  }

  @Test
  @SneakyThrows
  public void testC360SchemaInference() {
    ErrorCollector errors = ErrorCollector.root();
    String ds2Name = "c360";
    DirectorySourceImplementation  fileConfig = DirectorySourceImplementation.builder()
            .uri(C360Example.RETAIL_DATA_DIR.toAbsolutePath().toString())
            .build();
    registry.addOrUpdateSource(ds2Name, fileConfig, errors);
    assertFalse(errors.isFatal());

    SourceDataset ds = registry.getDataset(Name.system(ds2Name));
    assertEquals(3, ds.getTables().size());
    SourceTable customer = ds.getTable("customer");
    SourceTable orders = ds.getTable("orders");
    SourceTable product = ds.getTable("product");

    assertEquals(4,customer.getStatistics().getCount());
    assertEquals(6,product.getStatistics().getCount());
    assertEquals(4,orders.getStatistics().getCount());
  }


  @SneakyThrows
  @Test
  public void testDatasetMonitoring() {
    ErrorCollector errors = ErrorCollector.root();

    String dsName = "bookclub";
    DirectorySourceImplementation fileConfig = DirectorySourceImplementation.builder()
            .uri(ConfigurationTest.DATA_DIR.toAbsolutePath().toString())
            .build();
    registry.addOrUpdateSource(dsName, fileConfig, errors);
    System.out.println(errors);
    assertFalse(errors.isFatal());

    String ds2Name = "c360";
    fileConfig = DirectorySourceImplementation.builder()
            .uri(C360Example.RETAIL_DATA_DIR.toAbsolutePath().toString())
            .build();
    registry.addOrUpdateSource(ds2Name, fileConfig, errors);
    assertFalse(errors.isFatal());


    //Needs some time to wait for the flink pipeline to compile data
    Thread.sleep(2000);

    SourceDataset ds = registry.getDataset(Name.system(dsName));
    SourceTable book = ds.getTable("book");
    SourceTable person = ds.getTable("person");

    SourceTableStatistics stats = book.getStatistics();
    assertNotNull(stats);
    assertEquals(4,stats.getCount());
    assertEquals(5, person.getStatistics().getCount());

    ImportManager imports = new ImportManager(registry);
    FlinkInputHandlerProvider tbConverter = new FlinkInputHandlerProvider();

    ErrorCollector schemaErrs = ErrorCollector.root();
    ImportManager.SourceTableImport bookImp = imports.importTable(Name.system(dsName),Name.system("book"),
            SchemaAdjustmentSettings.DEFAULT,schemaErrs);
    ImportManager.SourceTableImport ordersImp = imports.importTable(Name.system(ds2Name),Name.system("orders"),
            SchemaAdjustmentSettings.DEFAULT,schemaErrs);

    LocalFlinkStreamEngineImpl flink = new LocalFlinkStreamEngineImpl();
    FlinkStreamEngine.Builder streamBuilder = flink.createStream();
    StreamExecutionEnvironment env = streamBuilder.getEnvironment();
    StreamTableEnvironmentImpl tEnv = (StreamTableEnvironmentImpl)StreamTableEnvironment.create(env);
    final OutputTag<SchemaValidationProcess.Error> schemaErrorTag = new OutputTag<>("SCHEMA_ERROR"){};

    ImportManager.SourceTableImport imp = ordersImp;
    FlinkInputHandler inputHandler = inputHandlerProvider.get(imp.getSourceSchema());

    DataStream<SourceRecord.Raw> stream = new DataStreamProvider().getDataStream(imp.getTable(),streamBuilder);
    SingleOutputStreamOperator<SourceRecord.Named> validate = stream.process(new SchemaValidationProcess(schemaErrorTag, imp.getSourceSchema(),
            SchemaAdjustmentSettings.DEFAULT, imp.getTable().getDataset().getDigest()));
    SingleOutputStreamOperator<Row> rows = validate.map(inputHandler.getMapper(),inputHandler.getTypeInformation());

    Table table = tEnv.fromDataStream(rows, inputHandler.getTableSchema());

    tEnv.createTemporaryView("TheTable", table);
    table.printSchema();

    Table tableShredding = tEnv.sqlQuery("SELECT  o._uuid, items._idx, o.customerid, items.discount, items.quantity, items.productid, items.unit_price \n" +
            "FROM TheTable o CROSS JOIN UNNEST(o.entries) AS items");

    tEnv.toChangelogStream(tableShredding).print();

    env.execute();
  }
}