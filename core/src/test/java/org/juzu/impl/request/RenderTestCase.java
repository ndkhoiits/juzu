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

package org.juzu.impl.request;

import org.juzu.test.AbstractInjectTestCase;
import org.juzu.test.request.MockApplication;
import org.juzu.test.request.MockClient;
import org.juzu.test.request.MockRenderBridge;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class RenderTestCase extends AbstractInjectTestCase
{

   public void testImplicitPrinter() throws Exception
   {
      MockApplication<?> app = application("request", "render", "implicitprinter").init();

      //
      MockClient client = app.client();
      MockRenderBridge render = client.render();
      assertEquals("done", render.getContent());
   }

   public void testExplicitPrinter() throws Exception
   {
      MockApplication<?> app = application("request", "render", "explicitprinter").init();

      //
      MockClient client = app.client();
      MockRenderBridge render = client.render();
      assertEquals("done", render.getContent());
   }
}
