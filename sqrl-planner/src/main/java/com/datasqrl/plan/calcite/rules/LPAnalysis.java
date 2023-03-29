package com.datasqrl.plan.calcite.rules;

import com.datasqrl.plan.calcite.hints.OptimizerHint;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import org.apache.calcite.rel.RelNode;

@Value
public class LPAnalysis {

  @NonNull RelNode originalRelnode;

  @NonNull AnnotatedLP convertedRelnode;

  @NonNull List<OptimizerHint.Stage> configuredStages;

  @NonNull SQRLConverter.Config converterConfig;


}
