/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.packager;

import com.datasqrl.AbstractQuerySQRLIT;
import com.datasqrl.IntegrationTestSettings;
import com.datasqrl.util.SnapshotTest;
import com.datasqrl.util.TestGraphQLSchema;
import com.datasqrl.util.TestScript;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class RunRepository extends AbstractQuerySQRLIT {

  @Test
  public void testQuery(Vertx vertx, VertxTestContext testContext) {
    fullScriptTest(DataSQRL.INSTANCE.getScript(), DataSQRL.INSTANCE.getGraphQL(), vertx,
        testContext);
  }

  public void fullScriptTest(TestScript script, TestGraphQLSchema graphQLSchema, Vertx vertx,
      VertxTestContext testContext) {
    this.vertx = vertx;
    this.vertxContext = testContext;
    snapshot = SnapshotTest.Snapshot.of(getClass(), script.getName(), graphQLSchema.getName());
    initialize(IntegrationTestSettings.getFlinkWithDB(), script.getRootPackageDirectory());
    validateSchemaAndQueries(script.getScript(), graphQLSchema.getSchema(),
        graphQLSchema.getQueries());
  }
}
