/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.juzu.impl.application;

import org.juzu.AmbiguousResolutionException;
import org.juzu.Phase;
import org.juzu.metadata.ApplicationDescriptor;
import org.juzu.metadata.ControllerMethod;

import java.util.List;

/**
 * Resolves a controller for a given input.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ControllerResolver
{

   /** . */
   private final List<ControllerMethod> methods;

   /** . */
   private final ApplicationDescriptor desc;

   public ControllerResolver(ApplicationDescriptor desc) throws NullPointerException
   {
      if (desc == null)
      {
         throw new NullPointerException("No null application descriptor accepted");
      }

      //
      this.desc = desc;
      this.methods = desc.getControllerMethods();
   }

   public ControllerMethod resolve(Phase phase, String methodId) throws AmbiguousResolutionException
   {
      ControllerMethod found = null;

      //

      //
      if (methodId != null)
      {
         for (ControllerMethod method : methods)
         {
            if (method.getId().equals(methodId))
            {
               found = method;
               break;
            }
         }
      }
      else if (phase == Phase.RENDER)
      {
         for (ControllerMethod method : methods)
         {
            if (
               method.getPhase() == Phase.RENDER &&
               method.getName().equals("index") &&
               method.getArgumentParameters().isEmpty())
            {
               if (desc.getDefaultController() == method.getType())
               {
                  return method;
               }
               else if (found == null)
               {
                  found = method;
               }
               else
               {
                  throw new AmbiguousResolutionException();
               }
            }
         }
      }

      //
      return found;
   }
}
