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
package org.infinispan.jopr.infinispan;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Discovery class for Infinispan engines
 *
 * @author Heiko W. Rupp
 */
public class InfinispanDiscovery implements ResourceDiscoveryComponent<InfinispanComponent> {


   // Assume a java5 jmx-remote connector on port 6996
   public static String REMOTE = "service:jmx:rmi://127.0.0.1/jndi/rmi://127.0.0.1:6996/jmxrmi";

   public static String MANAGER_OBJECT = "*:cache-name=[global],jmx-resource=CacheManager";

   String connector = "org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor";
   private final Log log = LogFactory.getLog(this.getClass());


   /**
    * Run the discovery
    */
   public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<InfinispanComponent> discoveryContext) throws Exception {

      Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

      /*
      * Currently this uses a hardcoded remote address for access to the MBean server
      * This needs to be switched to check if we e.g. run inside a JBossAS to which we
      * have a connection already that we can reuse.
      */
      Configuration c = new Configuration(); // TODO get from defaultPluginConfig
      c.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY, REMOTE));
      c.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE, connector));
      c.put(new PropertySimple("objectName", MANAGER_OBJECT));

      ConnectionHelper helper = new ConnectionHelper();
      EmsConnection conn = helper.getEmsConnection(c);

      // Run query for manager_object
      ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(MANAGER_OBJECT);
      List<EmsBean> beans = conn.queryBeans(queryUtility.getTranslatedQuery());

      for (EmsBean bean : beans) {

         String managerName = bean.getBeanName().getCanonicalName();
         c.put(new PropertySimple("objectName", managerName));
         /**
          *
          * A discovered resource must have a unique key, that must
          * stay the same when the resource is discovered the next
          * time
          */
         DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
               discoveryContext.getResourceType(), // ResourceType
               managerName, // Resource Key
               "Infinispan Cache Manager", // Resource Name
               null, // Version TODO can we get that from discovery ?
               "The Infinispan Manager", // Description
               c, // Plugin Config
               null // Process info from a process scan
         );


         // Add to return values
         discoveredResources.add(detail);
         log.info("Discovered Infinispan instance: " + managerName);
      }
      return discoveredResources;

   }
}