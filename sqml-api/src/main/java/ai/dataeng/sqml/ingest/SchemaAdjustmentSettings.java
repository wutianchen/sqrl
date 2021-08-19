package ai.dataeng.sqml.ingest;


import java.io.Serializable;

public interface SchemaAdjustmentSettings extends Serializable {

    public static final SchemaAdjustmentSettings DEFAULT = new SchemaAdjustmentSettings() {};

    default boolean singleton2Arrays() {
        return true;
    }

    default boolean removeListNulls() {
        return true;
    }

    default boolean array2Singleton() {
        return true;
    }

    default boolean null2EmptyArray() {
        return true;
    }

    default boolean castDataType() {
        return true;
    }

    default boolean dropFields() {
        return true;
    }

}
