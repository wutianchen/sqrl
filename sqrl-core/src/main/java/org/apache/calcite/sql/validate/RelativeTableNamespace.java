/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql.validate;

import ai.datasqrl.schema.Relationship;
import ai.datasqrl.schema.SQRLTable;
import java.util.List;
import java.util.Optional;
import ai.datasqrl.plan.local.transpile.TablePath;
import ai.datasqrl.plan.local.transpile.TablePathImpl;
import lombok.Getter;

/** Namespace based on a table from a table scope. */
@Getter
public class RelativeTableNamespace extends TableNamespace implements ExpandableTableNamespace {

  private final SQRLTable baseTable;
  private final String baseAlias;
  private final List<Relationship> relationships;

  RelativeTableNamespace(SqlValidatorImpl validator, SqlValidatorTable table, SQRLTable baseTable,
      String baseAlias, List<Relationship> relationships) {
    super(validator, table);
    this.baseTable = baseTable;
    this.baseAlias = baseAlias;
    this.relationships = relationships;
  }

  @Override
  public TablePath createTablePath(String alias) {
    return new TablePathImpl(baseTable, Optional.of(baseAlias), true, relationships, alias);
  }

  @Override
  public SQRLTable getDestinationTable() {
    return relationships.get(relationships.size()-1).getToTable();
  }
}
