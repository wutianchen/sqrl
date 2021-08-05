package ai.dataeng.sqml.type;

import static com.google.common.base.Preconditions.checkArgument;

import ai.dataeng.sqml.analyzer.Field;
import ai.dataeng.sqml.tree.Expression;
import ai.dataeng.sqml.tree.QualifiedName;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class SqmlType {
  protected final String name;

  private SqmlType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static abstract class ScalarSqmlType extends SqmlType {
    private ScalarSqmlType(String name) {
      super(name);
    }
  }

  public static class StringSqmlType extends ScalarSqmlType {
    public static StringSqmlType INSTANCE = new StringSqmlType();
    public StringSqmlType() {
      super("STRING");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitString(this, context);
    }
  }

  public static class NumberSqmlType extends ScalarSqmlType {
    public static NumberSqmlType INSTANCE = new NumberSqmlType();
    public NumberSqmlType(String name) {
      super(name);
    }
    public NumberSqmlType() {
      this("NUMBER");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitNumber(this, context);
    }
  }

  public static class IntegerSqmlType extends NumberSqmlType {
    public static IntegerSqmlType INSTANCE = new IntegerSqmlType();
    public IntegerSqmlType() {
      super("INTEGER");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitInteger(this, context);
    }
  }
  public static class FloatSqmlType extends NumberSqmlType {
    public static FloatSqmlType INSTANCE = new FloatSqmlType();
    public FloatSqmlType() {
      super("FLOAT");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitFloat(this, context);
    }
  }

  public static class BooleanSqmlType extends ScalarSqmlType {
    public static BooleanSqmlType INSTANCE = new BooleanSqmlType();
    public BooleanSqmlType() {
      super("BOOLEAN");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitBoolean(this, context);
    }
  }

  public static class ArraySqmlType extends SqmlType {

    private final SqmlType subType;

    public ArraySqmlType(SqmlType subType) {
      super("ARRAY");
      this.subType = subType;
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitArray(this, context);
    }

    public SqmlType getSubType() {
      return subType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ArraySqmlType that = (ArraySqmlType) o;
      return Objects.equals(subType, that.subType) && Objects.equals(name, that.getName());
    }

    @Override
    public int hashCode() {
      return Objects.hash(subType);
    }
  }

  public static class UnknownSqmlType extends SqmlType {
    public static UnknownSqmlType INSTANCE = new UnknownSqmlType();
    public UnknownSqmlType() {
      super("UNKNOWN");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitUnknown(this, context);
    }
  }

  public static class DateTimeSqmlType extends ScalarSqmlType {
    public static DateTimeSqmlType INSTANCE = new DateTimeSqmlType();
    public DateTimeSqmlType() {
      super("DATE");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitDateTime(this, context);
    }
  }
  public static class NullSqmlType extends ScalarSqmlType {
    public static NullSqmlType INSTANCE = new NullSqmlType();
    public NullSqmlType() {
      super("NULL");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitNull(this, context);
    }
  }

  public static class UuidSqmlType extends ScalarSqmlType {
    public static UuidSqmlType INSTANCE = new UuidSqmlType();
    public UuidSqmlType() {
      super("UUID");
    }
    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitUuid(this, context);
    }
  }

  public static class RelationSqmlType extends SqmlType {
    public static RelationSqmlType INSTANCE = new RelationSqmlType();
    private List<Field> fields;
    private Optional<Expression> expression = Optional.empty();

    public RelationSqmlType(Field... fields) {
      super("RELATION");
      this.fields = new ArrayList<>(List.of(fields));
    }

    public RelationSqmlType(List<Field> fields) {
      super("RELATION");
      this.fields = fields;
    }

    public <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context) {
      return visitor.visitRelation(this, context);
    }

    public List<Field> getFields() {
      return fields;
    }

    public List<Field> resolveFields(QualifiedName name) {
      //Todo: resolve scoped field: e.g. @.x
      return fields.stream()
          .filter(f->matches(f, name))
          .collect(Collectors.toList());
    }

    public Optional<Expression> getExpression() {
      return expression;
    }

    public Optional<Field> resolveField(QualifiedName name) {
      if (name.toString().contains("state")) {
        System.out.println();
      }
      RelationSqmlType rel = this;

      List<String> parts = name.getParts();
      for (int i = 0; i < parts.size() - 1; i++) {
        String part = parts.get(i);
        if (part.equalsIgnoreCase("@")) {
          throw new RuntimeException("TBD");
        }
        Optional<Field> field = rel.getField(part);
        if (field.isEmpty()) {
          return Optional.empty();
        }
        Field f = field.get();
        if (!(f.getType() instanceof RelationSqmlType)) {
          throw new RuntimeException(String.format("Field is not a relation %s", name));
        }
        rel = (RelationSqmlType)f.getType();
      }

      return rel.getField(name.getSuffix());
    }

    private Optional<Field> getField(String part) {
      for (Field field : fields) {
        if (matches(field, QualifiedName.of(part))) {
          return Optional.of(field);
        }
      }
      return Optional.empty();
    }

    private boolean matches(Field f, QualifiedName name) {
      boolean matchesFieldName = f.getName().get().equalsIgnoreCase(name.getSuffix());
      if (!matchesFieldName) {
        return false;
      }
      //must match alias
      if (f.getRelationAlias().isPresent()) {
        if (name.getPrefix().isEmpty()) {
          return false;
        }

        QualifiedName alias = f.getRelationAlias().get();
        return alias.equals(name.getPrefix().get());
      }

      return true;
    }

    public void addField(Field field) {
      //todo shadow fields
      for (int i = fields.size() - 1; i >= 0; i--) {
        Field f = fields.get(i);
        if (f.getName().get().equalsIgnoreCase(field.getName().get())) {
          fields.remove(i);
        }
      }

      this.fields.add(field);
    }

    public RelationSqmlType join(RelationSqmlType right) {
      List<Field> joinFields = new ArrayList<>();
      joinFields.addAll(this.fields);
      joinFields.addAll(right.getFields());

      return new RelationSqmlType(joinFields);
    }

    public RelationSqmlType withAlias(String relationAlias) {

      ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
      for (int i = 0; i < this.fields.size(); i++) {
        Field field = this.fields.get(i);
        Optional<String> columnAlias = field.getName();
          fieldsBuilder.add(Field.newQualified(
              QualifiedName.of(relationAlias),
              columnAlias,
              field.getType(),
              field.isHidden(),
              field.getOriginTable(),
              field.getOriginColumnName(),
              field.isAliased()));
      }

      return new RelationSqmlType(fieldsBuilder.build());
    }

    public void setExpression(Optional<Expression> expression) {
      this.expression = expression;
    }
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SqmlType type = (SqmlType) o;
    return name.equals(type.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public abstract <R, C> R accept(SqmlTypeVisitor<R, C> visitor, C context);
}