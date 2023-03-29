/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl;

import com.datasqrl.config.EngineSettings;
import com.datasqrl.engine.PhysicalPlan;
import com.datasqrl.engine.PhysicalPlan.StagePlan;
import com.datasqrl.engine.PhysicalPlanExecutor;
import com.datasqrl.engine.database.QueryTemplate;
import com.datasqrl.engine.database.relational.JDBCEngine;
import com.datasqrl.engine.database.relational.JDBCEngineConfiguration;
import com.datasqrl.engine.pipeline.ExecutionPipeline;
import com.datasqrl.error.ErrorCollector;
import com.datasqrl.frontend.SqrlPhysicalPlan;
import com.datasqrl.io.impl.file.DirectoryDataSystem.DirectoryConnector;
import com.datasqrl.io.impl.file.FilePath;
import com.datasqrl.loaders.SqrlModule;
import com.datasqrl.name.NamePath;
import com.datasqrl.plan.calcite.table.VirtualRelationalTable;
import com.datasqrl.plan.calcite.util.RelToSql;
import com.datasqrl.plan.global.PhysicalDAGPlan;
import com.datasqrl.plan.global.PhysicalDAGPlan.ExternalSink;
import com.datasqrl.plan.global.PhysicalDAGPlan.WriteQuery;
import com.datasqrl.plan.local.analyze.ResolveTest;
import com.datasqrl.plan.local.analyze.RetailSqrlModule;
import com.datasqrl.plan.local.generate.Namespace;
import com.datasqrl.plan.queries.APIQuery;
import com.datasqrl.util.DatabaseHandle;
import com.datasqrl.util.FileTestUtil;
import com.datasqrl.util.ResultSetPrinter;
import com.datasqrl.util.SnapshotTest;
import com.datasqrl.util.StreamUtil;
import com.datasqrl.util.TestRelWriter;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.RelNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

public class AbstractPhysicalSQRLIT extends AbstractLogicalSQRLIT {

  public SqrlPhysicalPlan physicalPlanner;
  JDBCEngineConfiguration jdbc;

  protected SnapshotTest.Snapshot snapshot;
  protected boolean closeSnapshotOnValidate = true;

  protected void initialize(IntegrationTestSettings settings, Path rootDir) {
    initialize(settings, rootDir, Optional.empty());
  }
  protected void initialize(IntegrationTestSettings settings, Path rootDir, Optional<Path> errorDir) {
    Map<NamePath, SqrlModule> addlModules = Map.of();
    if (rootDir == null) {
      addlModules = Map.of(NamePath.of("ecommerce-data"), new RetailSqrlModule());
    }
    Pair<DatabaseHandle, EngineSettings> engines = settings.getSqrlSettings();
    this.engineSettings = engines.getRight();
    this.database = engines.getLeft();

    SqrlTestDIModule module = new SqrlTestDIModule(engineSettings.getPipeline(), settings, rootDir, addlModules, errorDir,
        ErrorCollector.root());
    Injector injector = Guice.createInjector(module);

    super.initialize(settings, rootDir, injector);

    jdbc = injector.getInstance(ExecutionPipeline.class).getStages().stream()
      .filter(f->f.getEngine() instanceof JDBCEngine)
      .map(f->((JDBCEngine) f.getEngine()).getConfig())
      .findAny()
      .orElseThrow();

    physicalPlanner = injector.getInstance(SqrlPhysicalPlan.class);
  }


  protected void validateTables(String script, String... queryTables) {
    validateTables(script, Collections.EMPTY_SET, queryTables);
  }

  protected void validateTables(String script, Set<String> tableWithoutTimestamp,
      String... queryTables) {
    validateTables(script, Arrays.asList(queryTables), tableWithoutTimestamp, Set.of());
  }

  @SneakyThrows
  protected void validateTables(String script, Collection<String> queryTables,
      Set<String> tableWithoutTimestamp, Set<String> tableNoDataSnapshot) {
    Namespace ns = plan(script);

    //We add a scan query for every query table
    List<APIQuery> queries = new ArrayList<APIQuery>();
    CalciteSchema relSchema = planner.getSchema();
    for (String tableName : queryTables) {
      Optional<VirtualRelationalTable> vtOpt = ResolveTest.getLatestTable(relSchema, tableName,
          VirtualRelationalTable.class);
      Preconditions.checkArgument(vtOpt.isPresent(), "No such table: %s", tableName);
      VirtualRelationalTable vt = vtOpt.get();
      RelNode rel = planner.createRelBuilder().scan(vt.getNameId()).build();
      queries.add(new APIQuery(tableName, rel));
    }

    PhysicalDAGPlan dag = physicalPlanner.planDag(ns, queries);
    addContent(dag);

    PhysicalPlan physicalPlan = physicalPlanner.createPhysicalPlan(dag);
    PhysicalPlanExecutor executor = new PhysicalPlanExecutor();
    PhysicalPlanExecutor.Result result = executor.execute(physicalPlan);
    //todo: filter out jdbc engine
    StagePlan db = physicalPlan.getStagePlans().stream()
        .filter(e-> e.getStage().getEngine() instanceof JDBCEngine)
        .findAny()
        .orElseThrow();
    JDBCEngine dbEngine = (JDBCEngine) db.getStage().getEngine();
    Connection conn = DriverManager.getConnection(dbEngine.getConfig().getConfig()
        .getDbURL(), dbEngine.getConfig().getConfig().getUser(),
        dbEngine.getConfig().getConfig().getPassword());

    for (APIQuery query : queries) {
      QueryTemplate template = physicalPlan.getDatabaseQueries().get(query);
      String sqlQuery = RelToSql.convertToSql(template.getRelNode());
      System.out.println("Executing query for " + query.getNameId() + ": " + sqlQuery);

      ResultSet resultSet = conn
          .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
          .executeQuery(sqlQuery);
      if (tableNoDataSnapshot.contains(query.getNameId())) {
        resultSet.last();
        int numResults = resultSet.getRow();
        System.out.println("Number of rows returned: " + numResults);
      } else {
        //Since Flink execution order is non-deterministic we need to sort results and remove uuid and ingest_time which change with every invocation
        Predicate<Integer> typeFilter = Predicates.alwaysTrue();
        if (tableWithoutTimestamp.contains(query.getNameId())) {
          typeFilter = filterOutTimestampColumn;
        }
        String content = Arrays.stream(ResultSetPrinter.toLines(resultSet,
                s -> Stream.of("_uuid", "_ingest_time", "__").noneMatch(p -> s.startsWith(p)),
                typeFilter))
            .sorted().collect(Collectors.joining(System.lineSeparator()));
        snapshot.addContent(content, query.getNameId(), "data");
      }
    }
    StreamUtil.filterByClass(dag.getQueriesByType(PhysicalDAGPlan.WriteQuery.class).stream()
        .map(WriteQuery::getSink),ExternalSink.class)
        .map(ExternalSink::getTableSink)
        .filter(sink -> sink.getConnector() instanceof DirectoryConnector)
        .forEach(sink -> {
          DirectoryConnector connector = (DirectoryConnector) sink.getConnector();
          FilePath path = connector.getPathConfig().getDirectory()
              .resolve(sink.getConfiguration().getIdentifier());
          Path filePath = Paths.get(path.toString());
          snapshot.addContent(String.valueOf(FileTestUtil.countLinesInAllPartFiles(filePath)),
              "export", sink.getConfiguration().getIdentifier());
        });
    if (closeSnapshotOnValidate) snapshot.createOrValidate();
  }

  private void addContent(PhysicalDAGPlan dag, String... caseNames) {
    dag.getWriteQueries().stream().sorted(Comparator.comparing(wq -> wq.getSink().getName()))
        .forEach(mq -> snapshot.addContent(TestRelWriter.explain(mq.getRelNode()),
        ArrayUtils.addAll(caseNames, mq.getSink().getName(), "lp-stream")));
    dag.getReadQueries().stream().sorted(Comparator.comparing(rq -> rq.getQuery().getNameId()))
        .forEach(dq -> snapshot.addContent(TestRelWriter.explain(dq.getRelNode()),
        ArrayUtils.addAll(caseNames, dq.getQuery().getNameId(), "lp-database")));
  }

  protected static final Predicate<Integer> filterOutTimestampColumn =
      type -> type != Types.TIMESTAMP_WITH_TIMEZONE && type != Types.TIMESTAMP;


}
