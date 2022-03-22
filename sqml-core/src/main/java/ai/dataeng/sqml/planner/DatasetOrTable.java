package ai.dataeng.sqml.planner;

import ai.dataeng.sqml.planner.operator.ShadowingContainer;
import ai.dataeng.sqml.tree.name.VersionedName;

/**
 * Tables can be imported directly into the root scope of a script or an entire dataset (with all
 * tables) is imported and tables must be referenced through that dataset.
 */
public interface DatasetOrTable extends ShadowingContainer.Nameable {
  VersionedName getId();
}
