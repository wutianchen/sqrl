/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.engine.stream.flink;

import static com.datasqrl.engine.EngineCapability.*;

import com.datasqrl.engine.EngineCapability;
import com.datasqrl.engine.EnginePhysicalPlan;
import com.datasqrl.engine.ExecutionEngine;
import com.datasqrl.engine.ExecutionResult;
import com.datasqrl.engine.stream.StreamEngine;
import com.datasqrl.engine.stream.flink.plan.FlinkPhysicalPlanner;
import com.datasqrl.engine.stream.flink.plan.FlinkStreamPhysicalPlan;
import com.datasqrl.engine.stream.monitor.DataMonitor;
import com.datasqrl.io.tables.TableSink;
import com.datasqrl.plan.global.PhysicalDAGPlan;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import org.apache.calcite.tools.RelBuilder;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.internal.FlinkEnvProxy;

public abstract class AbstractFlinkStreamEngine extends ExecutionEngine.Base implements
    StreamEngine {

  public static final EnumSet<EngineCapability> FLINK_CAPABILITIES = STANDARD_STREAM;
  final FlinkEngineConfiguration config;

  public AbstractFlinkStreamEngine(FlinkEngineConfiguration config) {
    super(FlinkEngineConfiguration.ENGINE_NAME, Type.STREAM, FLINK_CAPABILITIES);
    this.config = config;
  }

  @Override
  public ExecutionResult execute(EnginePhysicalPlan plan) {
    Preconditions.checkArgument(plan instanceof FlinkStreamPhysicalPlan);
    FlinkStreamPhysicalPlan flinkPlan = (FlinkStreamPhysicalPlan) plan;
    StatementSet statementSet = flinkPlan.getStatementSet();
    flinkPlan.getJars().forEach(j->FlinkEnvProxy.addJar(statementSet, j.toString()));
    TableResult rslt = statementSet.execute();
    rslt.print(); //todo: this just forces print to wait for the async
    return new ExecutionResult.Message(rslt.getJobClient().get()
        .getJobID().toString());
  }

  @Override
  public FlinkStreamPhysicalPlan plan(PhysicalDAGPlan.StagePlan plan,
      List<PhysicalDAGPlan.StageSink> inputs, RelBuilder relBuilder, TableSink errorSink) {
    Preconditions.checkArgument(inputs.isEmpty());
    FlinkStreamPhysicalPlan streamPlan = new FlinkPhysicalPlanner(this).createStreamGraph(
        plan.getQueries(), errorSink, plan.getJars());
    return streamPlan;
  }

  public abstract FlinkStreamBuilder createJob();

  @Override
  public DataMonitor createDataMonitor() {
    return createJob();
  }

  @Override
  public void close() throws IOException {
//    jobs.clear();
  }

}
