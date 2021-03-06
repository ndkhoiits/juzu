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

import org.juzu.Controller;
import org.juzu.Phase;
import org.juzu.Response;
import org.juzu.impl.inject.Export;
import org.juzu.impl.inject.ScopeController;
import org.juzu.impl.spi.inject.InjectManager;
import org.juzu.impl.spi.request.ActionBridge;
import org.juzu.impl.spi.request.RenderBridge;
import org.juzu.impl.request.Request;
import org.juzu.impl.spi.request.RequestBridge;
import org.juzu.impl.spi.request.ResourceBridge;
import org.juzu.metadata.ApplicationDescriptor;
import org.juzu.metadata.ControllerMethod;
import org.juzu.metadata.ControllerParameter;
import org.juzu.request.ActionContext;
import org.juzu.request.ApplicationContext;
import org.juzu.request.MimeContext;
import org.juzu.request.RenderContext;
import org.juzu.request.RequestContext;
import org.juzu.impl.spi.template.TemplateStub;
import org.juzu.impl.utils.Spliterator;
import org.juzu.request.ResourceContext;
import org.juzu.template.Template;
import org.juzu.text.Printer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@Export
@Singleton
public class InternalApplicationContext extends ApplicationContext
{

   public static RequestContext getCurrentRequest()
   {
      return current.get().getContext();
   }

   /** . */
   private final ApplicationDescriptor descriptor;

   /** . */
   final InjectManager manager;

   /** . */
   private final ControllerResolver controllerResolver;

   /** . */
   static final ThreadLocal<Request> current = new ThreadLocal<Request>();

   @Inject
   public InternalApplicationContext(InjectManager manager, ApplicationDescriptor descriptor)
   {
      this.descriptor = descriptor;
      this.manager = manager;
      this.controllerResolver = new ControllerResolver(descriptor);
   }

   public ApplicationDescriptor getDescriptor()
   {
      return descriptor;
   }

   public void invoke(RequestBridge bridge)
   {
      ClassLoader classLoader = manager.getClassLoader();

      //
      Phase phase;
      if (bridge instanceof RenderBridge)
      {
         phase = Phase.RENDER;
      }
      else if (bridge instanceof ActionBridge)
      {
         phase = Phase.ACTION;
      }
      else if (bridge instanceof ResourceBridge)
      {
         phase = Phase.RESOURCE;
      }
      else
      {
         throw new AssertionError();
      }
      ControllerMethod method = controllerResolver.resolve(phase, bridge.getMethodId());

      //
      Request request = new Request(method, classLoader, bridge);

      //
      ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
      try
      {
         Thread.currentThread().setContextClassLoader(classLoader);
         current.set(request);
         ScopeController.begin(request);
         Object ret = doInvoke(manager, request, method);
         if (phase == Phase.ACTION && ret != null && ret instanceof Response)
         {
            try
            {
               ((ActionBridge)bridge).setResponse((Response)ret);
            }
            catch (IOException e)
            {
               throw new UnsupportedOperationException("handle me gracefully");
            }
         }
      }
      finally
      {
         current.set(null);
         ScopeController.end();
         Thread.currentThread().setContextClassLoader(oldCL);
      }
   }

   public Object resolveBean(String name)
   {
      return resolveBean(manager, name);
   }

   private <B, I> Object resolveBean(InjectManager<B, I> manager, String name)
   {
      B bean = manager.resolveBean(name);
      if (bean != null)
      {
         I cc = manager.create(bean);
         return manager.get(bean, cc);
      }
      else
      {
         return null;
      }
   }

   private <B, I> Object doInvoke(InjectManager<B, I> manager, Request request, ControllerMethod method)
   {
      RequestContext context = request.getContext();

      //
      if (method == null)
      {
         StringBuilder sb = new StringBuilder("handle me gracefully : no method could be resolved for " +
            "phase=" + context.getPhase() + " and parameters={");
         int index = 0;
         for (Map.Entry<String, String[]> entry : context.getParameters().entrySet())
         {
            if (index++ > 0)
            {
               sb.append(',');
            }
            sb.append(entry.getKey()).append('=').append(Arrays.asList(entry.getValue()));
         }
         sb.append("}");
         throw new UnsupportedOperationException(sb.toString());
      }
      else
      {
         Class<?> type = method.getType();
         B bean = manager.resolveBean(type);

         if (bean != null)
         {
            I instance = null;
            try
            {
               // Get the bean
               instance = manager.create(bean);

               // Get a reference
               Object o = manager.get(bean, instance);

               //
               if (o instanceof Controller)
               {
                  Controller controller = (Controller)o;
                  switch (request.getContext().getPhase())
                  {
                     case ACTION:
                        return controller.processAction((ActionContext)context);
                     case RENDER:
                        controller.render((RenderContext)context);
                        return null;
                     case RESOURCE:
                        controller.serveResource((ResourceContext)context);
                        return null;
                     default:
                        throw new AssertionError();
                  }
               }
               else
               {
                  // Prepare method parameters
                  List<ControllerParameter> params = method.getArgumentParameters();
                  Object[] args = new Object[params.size()];
                  for (int i = 0;i < args.length;i++)
                  {
                     String[] values = context.getParameters().get(params.get(i).getName());
                     args[i] = (values != null && values.length > 0) ? values[0] : null;
                  }

                  //
                  return method.getMethod().invoke(o, args);
               }
            }
            catch (Exception e)
            {
               throw new UnsupportedOperationException("handle me gracefully", e);
            }
            finally
            {
               if (instance != null)
               {
                  manager.release(instance);
               }
            }
         }
         else
         {
            return null;
         }
      }
   }

   public Printer getPrinter()
   {
      Request req = current.get();
      RequestContext context = req.getContext();
      if (context instanceof MimeContext)
      {
         return ((MimeContext)context).getPrinter();
      }
      else
      {
         throw new AssertionError("does not make sense");
      }
   }

   public TemplateStub resolveTemplateStub(String path)
   {
      try
      {
         StringBuilder id = new StringBuilder(descriptor.getTemplatesPackageName());
         String relativePath = path.substring(0, path.indexOf('.'));
         for (String name : Spliterator.split(relativePath, '/'))
         {
            if (id.length() > 0)
            {
               id.append('.');
            }
            id.append(name);
         }
         ClassLoader cl = manager.getClassLoader();
         Class<?> stubClass = cl.loadClass(id.toString());
         return(TemplateStub)stubClass.newInstance();
      }
      catch (Exception e)
      {
         throw new UnsupportedOperationException("handle me gracefully", e);
      }
   }

   @Override
   public void render(Template template, Printer printer, Map<String, ?> attributes, Locale locale) throws IOException
   {
      Printer toUse = printer != null ? printer : getPrinter();

      //
      TemplateStub stub = resolveTemplateStub(template.getPath());

      //
      ApplicationTemplateRenderContext context = new ApplicationTemplateRenderContext(this, toUse, attributes, locale);

      //
      stub.render(context);

      //
      String title = context.getTitle();
      if (printer == null && title != null)
      {
         RequestContext ctx = current.get().getContext();
         if (ctx instanceof RenderContext)
         {
            ((RenderContext)ctx).setTitle(title);
         }
      }
   }
}
