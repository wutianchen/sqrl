/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.engine.stream.flink;

import com.datasqrl.error.ErrorCollector;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ProcessError<Input> implements Serializable {

  private List<String> errors;
  private Input input;

  public static <Input> ProcessError<Input> of(ErrorCollector errors, Input input) {
    List<String> errorMsgs = new ArrayList<>();
    errors.forEach(e -> errorMsgs.add(e.toString()));
    return new ProcessError(errorMsgs, input);
  }

}
