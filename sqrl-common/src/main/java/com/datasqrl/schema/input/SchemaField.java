/*
 * Copyright (c) 2021, DataSQRL. All rights reserved. Use is subject to license terms.
 */
package com.datasqrl.schema.input;

import com.datasqrl.name.Name;
import java.io.Serializable;

public interface SchemaField extends Serializable {

  Name getName();
}
