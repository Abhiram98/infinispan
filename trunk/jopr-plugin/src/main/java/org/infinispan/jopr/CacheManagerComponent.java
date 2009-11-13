/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.jopr;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

import java.util.Set;

/**
 * The component class for the Infinispan manager
 *
 * @author Heiko W. Rupp
 * @author Galder Zamarreño
 */
public class CacheManagerComponent implements ResourceComponent, MeasurementFacet {
   private static final Log log = LogFactory.getLog(CacheManagerComponent.class);
   private ResourceContext context;
   private ConnectionHelper helper;

   /**
    * Return availability of this resource. We do this by checking the connection to it. If the Manager would expose
    * some "run state" we could check for that too.
    *
    * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
    */
   public AvailabilityType getAvailability() {
      boolean trace = log.isTraceEnabled();
      EmsConnection conn = getConnection();
      try {
         conn.refresh();
         EmsBean bean = queryCacheManagerBean(conn);
         if (bean != null) {
            bean.refreshAttributes();
            if (trace) log.trace("Cache manager could be found and attributes where refreshed, so it's up.", bean);
            return AvailabilityType.UP;
         }
         if (trace) log.trace("Cache manager could not be found, so cache manager is down");
         return AvailabilityType.DOWN;
      } catch (Exception e) {
         if (trace) log.trace("There was an exception checking availability, so cache manager is down");
         return AvailabilityType.DOWN;
      }
   }

   /**
    * Start the resource connection
    *
    * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
    */
   public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
      this.context = context;
      this.helper = new ConnectionHelper();
      getConnection();
   }

   /**
    * Tear down the rescource connection
    *
    * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
    */
   public void stop() {
      helper.closeConnection();

   }

   /**
    * Gather measurement data
    *
    * @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport,
    *      java.util.Set)
    */
   public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
      boolean trace = log.isTraceEnabled();
      if (trace) log.trace("Get values for these metrics: {0}", metrics);
      EmsConnection conn = getConnection();
      if (trace) log.trace("Connection to ems server stablished: {0}", conn);
      EmsBean bean = queryCacheManagerBean(conn);
      bean.refreshAttributes();
      if (trace) log.trace("Querying returned bean: {0}", bean);
      for (MeasurementScheduleRequest req : metrics) {
         DataType type = req.getDataType();
         if (type == DataType.MEASUREMENT) {
            String tmp = (String) bean.getAttribute(req.getName()).getValue();
            Double val = Double.valueOf(tmp);
            if (trace) log.trace("Metric ({0}) is measurement with value {1}", req.getName(), val);
            MeasurementDataNumeric res = new MeasurementDataNumeric(req, val);
            report.addData(res);
         } else if (type == DataType.TRAIT) {
            String value = (String) bean.getAttribute(req.getName()).getValue();
            if (trace) log.trace("Metric ({0}) is trait with value {1}", req.getName(), value);
            MeasurementDataTrait res = new MeasurementDataTrait(req, value);
            report.addData(res);
         }
      }
   }

   /**
    * Helper to obtain a connection
    *
    * @return EmsConnection object
    */
   protected EmsConnection getConnection() {
      EmsConnection conn = helper.getEmsConnection(context.getPluginConfiguration());
      return conn;
   }

   private EmsBean queryCacheManagerBean(EmsConnection conn) {
      String pattern = context.getPluginConfiguration().getSimpleValue("objectName", null);
      if (log.isTraceEnabled()) log.trace("Pattern to query is {0}", pattern);
      ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(pattern);
      return conn.queryBeans(queryUtility.getTranslatedQuery()).get(0);
   }
}