package com.datasqrl.plan.local.generate;

import com.datasqrl.error.ErrorCollector;
import com.datasqrl.name.NameCanonicalizer;
import com.datasqrl.plan.calcite.table.CalciteTableFactory;

public class QueryStatementResolver extends AbstractQueryStatementResolver {

  protected QueryStatementResolver(ErrorCollector errors,
      NameCanonicalizer nameCanonicalizer, SqrlQueryPlanner planner, CalciteTableFactory tableFactory) {
    super(errors, nameCanonicalizer, planner, tableFactory);
  }

}
