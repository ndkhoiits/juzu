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

package org.juzu.test.request;

import org.json.JSONException;
import org.json.JSONObject;
import org.juzu.Phase;
import org.juzu.metadata.ControllerMethod;
import org.juzu.test.AbstractTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * A conversation between a client and the application.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class MockClient
{

   private MockRequestBridge create(String url)
   {
      MockRequestBridge request;
      ControllerMethod method;
      JSONObject json;
      try
      {
         json = new JSONObject(url);
         method = application.getContext().getDescriptor().getControllerMethodById(json.getString("op"));
         Phase phase = method.getPhase();
         String methodId = method.getId();
         switch (phase)
         {
            case ACTION:
               request = new MockActionBridge(this, methodId);
               break;
            case RENDER:
               request =  new MockRenderBridge(this, methodId);
               break;
            case RESOURCE:
               request =  new MockResourceBridge(this, methodId);
               break;
            default:
               throw AbstractTestCase.failure("Not yet supported " + phase);
         }
      }
      catch (JSONException e)
      {
         throw AbstractTestCase.failure(e);
      }

      //
      try
      {
         JSONObject jsonParams = json.getJSONObject("parameters");
         for (Iterator i = jsonParams.keys();i.hasNext();)
         {
            String name = (String)i.next();
            String value = jsonParams.getString(name);
            request.getParameters().put(name, new String[]{value});
         }
      }
      catch (JSONException e)
      {
         throw AbstractTestCase.failure(e);
      }

      //
      return request;
   }

   /** . */
   final MockApplication<?> application;

   /** . */
   private final Map<Object, Object> session;

   /** . */
   private Map<Object, Object> flash;

   /** . */
   private final LinkedList<Map<Object, Object>> flashHistory;

   public MockClient(MockApplication<?> application)
   {
      this.application = application;
      this.session = new HashMap<Object, Object>();
      this.flash  = null;
      this.flashHistory = new LinkedList<Map<Object, Object>>();
   }

   public MockRenderBridge render()
   {
      MockRenderBridge render = new MockRenderBridge(this);
      invoke(render);
      return render;
   }

   public MockRequestBridge invoke(String url)
   {
      MockRequestBridge request = create(url);
      invoke(request);
      return request;
   }

   public Object getFlashValue(Object key)
   {
      return flash != null ? flash.get(key) : null;
   }

   public void setFlashValue(Object key, Object value)
   {
      if (flash == null)
      {
         flash = new HashMap<Object, Object>();
      }
      flash.put(key, value);
   }

   private void invoke(MockRequestBridge request)
   {
      if (request instanceof MockActionBridge)
      {
         application.invoke(request);
      }
      else if (request instanceof MockRenderBridge)
      {
         application.invoke(request);
         flashHistory.addFirst(flash != null ? flash : Collections.emptyMap());
         flash = null;
      }
      else
      {
         application.invoke(request);
      }
   }

   public Map<Object, Object> getFlash()
   {
      return flash;
   }

   public Map<Object, Object> getFlash(int index)
   {
      if (index < 0)
      {
         throw new IndexOutOfBoundsException("Wrong index " + index);
      }
      if (index == 0)
      {
         return flash;
      }
      else
      {
         return flashHistory.get(index - 1);
      }
   }

   public Map<Object, Object> getSession()
   {
      return session;
   }
}
