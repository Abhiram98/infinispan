/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of
 * individual contributors.
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
package org.infinispan.jmx;

import java.util.Set;

import javax.management.MBeanServer;

import org.infinispan.CacheException;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.factories.AbstractComponentRegistry;
import org.infinispan.util.Util;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Parent class for top level JMX component registration.
 * 
 * @author Galder Zamarreño
 * @since 4.0
 */
public abstract class AbstractJmxRegistration {
   private static final Log log = LogFactory.getLog(AbstractJmxRegistration.class);
   String jmxDomain;
   MBeanServer mBeanServer;
   GlobalConfiguration globalConfig;
   
   protected abstract ComponentsJmxRegistration buildRegistrator(Set<AbstractComponentRegistry.Component> components);
   
   protected void registerMBeans(Set<AbstractComponentRegistry.Component> components, GlobalConfiguration globalConfig) {
      mBeanServer = getMBeanServer(globalConfig);
      ComponentsJmxRegistration registrator = buildRegistrator(components);
      registrator.registerMBeans();
   }
   
   protected void unregisterMBeans(Set<AbstractComponentRegistry.Component> components) {
      ComponentsJmxRegistration registrator = buildRegistrator(components);
      registrator.unregisterMBeans();
   }
   
   protected MBeanServer getMBeanServer(GlobalConfiguration configuration) {
      String serverLookup = configuration.getMBeanServerLookup();
      try {
         MBeanServerLookup lookup = (MBeanServerLookup) Util.getInstance(serverLookup);
         return lookup.getMBeanServer();
      } catch (Exception e) {
         log.error("Could not instantiate MBeanServerLookup('" + serverLookup + "')", e);
         throw new CacheException(e);
      }
   }
   
   protected String getJmxDomain(String jmxDomain, MBeanServer mBeanServer) {
      String[] registeredDomains = mBeanServer.getDomains();
      int index = 2;
      String finalName = jmxDomain;
      boolean done = false;
      while (!done) {
         done = true;
         for (String domain : registeredDomains) {
            if (domain.equals(finalName)) {
               finalName = jmxDomain + index++;
               done = false;
               break;
            }
         }
      }
      return finalName;
   }
}
