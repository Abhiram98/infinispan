package org.infinispan.query.dsl.embedded.impl;

import org.infinispan.AdvancedCache;
import org.infinispan.objectfilter.ObjectFilter;
import org.infinispan.objectfilter.impl.aggregation.FieldAccumulator;
import org.infinispan.objectfilter.impl.aggregation.Grouper;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author anistor@redhat.com
 * @since 8.0
 */
final class AggregatingQuery extends HybridQuery {

   private final int[] groupFieldPositions;

   private final FieldAccumulator[] accumulators;

   AggregatingQuery(QueryFactory queryFactory, AdvancedCache<?, ?> cache, String jpaQuery, Map<String, Object> namedParameters,
                    int[] groupFieldPositions, FieldAccumulator[] accumulators,
                    ObjectFilter objectFilter,
                    long startOffset, int maxResults,
                    Query baseQuery) {
      super(queryFactory, cache, jpaQuery, namedParameters, objectFilter, startOffset, maxResults, baseQuery);
      this.groupFieldPositions = groupFieldPositions;
      this.accumulators = accumulators;
   }

   @Override
   protected Iterator<?> getBaseIterator() {
      Grouper grouper = new Grouper(groupFieldPositions, accumulators);
      List<Object[]> list = baseQuery.list();
      for (Object[] row : list) {
         grouper.addRow(row);
      }
      return grouper.finish();
   }

   @Override
   public String toString() {
      return "AggregatingQuery{" +
            "jpaQuery=" + jpaQuery +
            ", namedParameters=" + namedParameters +
            ", groupFieldPositions=" + Arrays.toString(groupFieldPositions) +
            ", accumulators=" + Arrays.toString(accumulators) +
            ", projection=" + Arrays.toString(projection) +
            ", startOffset=" + startOffset +
            ", maxResults=" + maxResults +
            ", baseQuery=" + baseQuery +
            '}';
   }
}
