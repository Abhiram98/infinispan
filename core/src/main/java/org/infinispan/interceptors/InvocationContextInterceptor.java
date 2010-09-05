/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.interceptors;


import org.infinispan.commands.VisitableCommand;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;

/**
 * @author Mircea.Markus@jboss.com
 */
public class InvocationContextInterceptor extends CommandInterceptor {

   @Override
   public Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
      return handleAll(ctx, command);
   }

   private Object handleAll(InvocationContext ctx, VisitableCommand command) throws Throwable {
      boolean suppressExceptions = false;

      if (trace) log.trace("Invoked with command " + command + " and InvocationContext [" + ctx + "]");
      if (ctx == null) throw new IllegalStateException("Null context not allowed!!");

      if (ctx.hasFlag(Flag.FAIL_SILENTLY)) {
         suppressExceptions = true;
      }

      try {
         return invokeNextInterceptor(ctx, command);
      }
      catch (Throwable th) {
         if (suppressExceptions) {
            log.trace("Exception while executing code, failing silently...", th);
            return null;
         } else {
            log.error("Execution error: ", th);
            throw th;
         }
      } finally {
         ctx.reset();
      }
   }
}