package ie.tcd.scss.busnotifier.config;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import java.util.Set;

public class Provider implements SchemaFilterProvider {
    @Override
    public SchemaFilter getCreateFilter() {
        return Filter.INSTANCE;
    }

    @Override
    public SchemaFilter getDropFilter() {
        return Filter.INSTANCE;
    }

    @Override
    public SchemaFilter getTruncatorFilter() {
        return Filter.INSTANCE;
    }

    @Override
    public SchemaFilter getMigrateFilter() {
        return Filter.INSTANCE;
    }

    @Override
    public SchemaFilter getValidateFilter() {
        return Filter.INSTANCE;
    }

    public static class Filter implements SchemaFilter {

        public static final SchemaFilter INSTANCE = new Filter();

        @Override
        public boolean includeNamespace(Namespace namespace) {
            return true;
        }

        @Override
        public boolean includeTable(Table table) {
            return !Set.of("stops", "stop_times", "trips", "calendar", "routes").contains(table.getName());
        }

        @Override
        public boolean includeSequence(Sequence sequence) {
            return true;
        }
    }
}
