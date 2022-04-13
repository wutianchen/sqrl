package ai.dataeng.sqml.parser;

import ai.dataeng.sqml.parser.sqrl.schema.SourceTableFactory;
import ai.dataeng.sqml.tree.Expression;
import ai.dataeng.sqml.tree.Identifier;
import ai.dataeng.sqml.tree.Query;
import ai.dataeng.sqml.tree.QuerySpecification;
import ai.dataeng.sqml.tree.SelectItem;
import ai.dataeng.sqml.tree.SingleColumn;
import ai.dataeng.sqml.tree.TableSubquery;
import ai.dataeng.sqml.tree.name.Name;
import ai.dataeng.sqml.tree.name.NamePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TableFactory {

  /**
   * Creates a table based on a subquery and derives its ppk and pk
   */
  public Table create(Query query) {
    List<Column> columns = new ArrayList<>();
    for (SelectItem selectItem : ((QuerySpecification)query.getQueryBody())
        .getSelect().getSelectItems()) {
      SingleColumn col = (SingleColumn) selectItem;
      Column column = getOrCreateColumn(col.getExpression(), col.getAlias());
      columns.add(column);
    }
    Table table = new Table(-1, Name.system("subquery"), null, true);
    for (Column column : columns) {
      table.addField(column);
    }

    PrimaryKeyDeriver primaryKeyDeriver = new PrimaryKeyDeriver();
    List<Integer> pos = primaryKeyDeriver.derive(query);

    for (Integer index : pos) {
      Column column = ((Column)table.getFields().get(index));
      column.setPrimaryKey(true);
    }

    return table;
  }

  /**
   * Creates a column while keeping track of column source
   */
  private Column getOrCreateColumn(Expression expression,
      Optional<Identifier> alias) {
    if (expression instanceof Identifier) {
      Identifier i = (Identifier) expression;
      Column column = new Column(alias.map(Identifier::getNamePath).orElseGet(i::getNamePath).getLast(),
          null, 0, null, 0, List.of(),
          false, false, Optional.empty(), false);
      column.setSource((Field)i.getResolved());

      return column;
    }

    Column column = new Column(alias.map(Identifier::getNamePath)
        .orElseGet(()->Name.system("unnamedColumn").toNamePath()).getLast(),
        null, 0, null, 0, List.of(),
        false, false, Optional.empty(), false);

    return column;
  }

  public Table create(NamePath namePath, Name table) {
    return new Table(SourceTableFactory.tableIdCounter.incrementAndGet(), namePath.getLast(), namePath, false);
  }
}
