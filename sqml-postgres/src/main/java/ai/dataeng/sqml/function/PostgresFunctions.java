package ai.dataeng.sqml.function;

import ai.dataeng.sqml.type.SqmlType;
import ai.dataeng.sqml.type.SqmlType.StringSqmlType;
import java.util.List;

public class PostgresFunctions {

  public static List<SqmlFunction> SqmlSystemFunctions = List.of(
      new SqmlFunction("concat", new SqmlType.StringSqmlType(), false),
      new SqmlFunction("math.abs", new SqmlType.NumberSqmlType(), false),
      new SqmlFunction("count", new SqmlType.IntegerSqmlType(), false),
      new SqmlFunction("casts.tointeger", new SqmlType.IntegerSqmlType(), false),
      new SqmlFunction("casts.toboolean", new SqmlType.BooleanSqmlType(), false),
      new SqmlFunction("casts.tofloat", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("casts.tostring", new SqmlType.StringSqmlType(), false),
      new SqmlFunction("casts.touuid", new SqmlType.UuidSqmlType(), false),
      new SqmlFunction("casts.totimestamp", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("collect_as_set", new SqmlType.ArraySqmlType(new StringSqmlType()), false), //todo subtype
      new SqmlFunction("sum", new SqmlType.NumberSqmlType(), false),
      new SqmlFunction("roundtoday", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("timefct.roundtomin", new SqmlType.DateTimeSqmlType(), false), //todo unite
      new SqmlFunction("timefct.roundtohour", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("timefct.dayofweek", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("timefct.hourofday", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("time.roundtoday", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("time.dayofweek", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("time.roundtoweek", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("time.roundtomonth", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("avg", new SqmlType.NumberSqmlType(), false),
      new SqmlFunction("max", new SqmlType.NumberSqmlType(), false),
      new SqmlFunction("iff", new SqmlType.StringSqmlType(), false),
      new SqmlFunction("now", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("math.log10", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("math.sqrt", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("stats.removeoutlier", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("stats.normal.variance", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("stats.truncated_normal.percentile", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("stats.poisson.percentile", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("time.parse", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("math.floor", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("col.multiply", new SqmlType.FloatSqmlType(), false),
      new SqmlFunction("time.roundtomin", new SqmlType.DateTimeSqmlType(), false),
      new SqmlFunction("frozen_lookup", new SqmlType.StringSqmlType(), false)
  );
}
