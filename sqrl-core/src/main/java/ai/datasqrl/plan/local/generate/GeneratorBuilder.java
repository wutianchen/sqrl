package ai.datasqrl.plan.local.generate;

import ai.datasqrl.config.error.ErrorCollector;
import ai.datasqrl.environment.ImportManager;
import ai.datasqrl.plan.calcite.PlannerFactory;
import ai.datasqrl.plan.calcite.SqrlTypeFactory;
import ai.datasqrl.plan.calcite.SqrlTypeSystem;
import ai.datasqrl.plan.calcite.sqrl.table.CalciteTableFactory;
import ai.datasqrl.plan.local.analyze.VariableFactory;
import ai.datasqrl.schema.input.SchemaAdjustmentSettings;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.JoinDeclarationContainerImpl;
import org.apache.calcite.sql.SqlNodeBuilderImpl;
import org.apache.calcite.sql.TableMapperImpl;
import org.apache.calcite.sql.UniqueAliasGeneratorImpl;

public class GeneratorBuilder {

  private static final RelDataTypeFactory relDataTypeFactory = new SqrlTypeFactory(
      new SqrlTypeSystem());

  public static Generator build(ImportManager importManager, ErrorCollector error) {
    Generator generator = new Generator(new CalciteTableFactory(relDataTypeFactory),
        SchemaAdjustmentSettings.DEFAULT,
        new PlannerFactory(CalciteSchema.createRootSchema(false, false).plus())
            .createPlanner(),
        importManager,
        new UniqueAliasGeneratorImpl(new HashSet<>()),
        new JoinDeclarationContainerImpl(),
        new SqlNodeBuilderImpl(),
        new TableMapperImpl(new HashMap<>()),
        error,
        new VariableFactory()
    );
    return generator;
  }
}
