package com.datasqrl.plan.local.generate;

import com.datasqrl.error.ErrorCollector;
import com.datasqrl.name.NameCanonicalizer;
import com.datasqrl.plan.calcite.rules.LPAnalysis;
import com.datasqrl.plan.calcite.table.CalciteTableFactory;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.Assignment;
import org.apache.calcite.sql.SqlNode;

public abstract class AbstractQueryStatementResolver extends AbstractStatementResolver {


  private final CalciteTableFactory tableFactory;

  protected AbstractQueryStatementResolver(ErrorCollector errors,
      NameCanonicalizer nameCanonicalizer, SqrlQueryPlanner planner, CalciteTableFactory tableFactory) {
    super(errors, nameCanonicalizer, planner);
    this.tableFactory = tableFactory;
  }

  protected boolean setOriginalFieldnames() {
    return true;
  }

  protected RelNode preprocessRelNode(RelNode relNode, Assignment statement) {
    return relNode;
  }

  public void resolve(Assignment statement, Namespace ns) {
    SqlNode sqlNode = transpile(statement, ns);
    RelNode relNode = plan(sqlNode);
    relNode = preprocessRelNode(relNode,statement);

    Converter converter = new Converter();
    LPAnalysis analyzedLP = converter.convert(planner, relNode, setOriginalFieldnames(), ns, statement.getHints(), errors);

    NamespaceObject table = tableFactory.createTable(planner, ns, statement.getNamePath(), analyzedLP, getContext(ns, statement.getNamePath()));
    ns.addNsObject(table);
  }

}
