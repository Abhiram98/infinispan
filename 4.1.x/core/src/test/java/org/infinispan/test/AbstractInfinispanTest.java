/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.infinispan.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;

/**
 * AbstractInfinispanTest is a superclass of all Infinispan tests. 
 * 
 * @author Vladimir Blagojevic
 * @since 4.0
 */
public class AbstractInfinispanTest {
   
   @AfterTest(alwaysRun=true)
   protected void nullifyInstanceFields() {
      for(Class<?> current = this.getClass();current.getSuperclass() != null; current = current.getSuperclass()) {
         Field[] fields = current.getDeclaredFields();
         for(Field f:fields) {
            try {               
               if(!Modifier.isStatic(f.getModifiers()) && !f.getDeclaringClass().isPrimitive()) {
                  f.setAccessible(true);
                  f.set(this, null);
               }
            } catch (Exception e) {} 
         }         
      }      
   }
}
