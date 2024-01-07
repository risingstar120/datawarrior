/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* this is stub code written based on Apple EAWT package javadoc published at
 * http://developer.apple.com.  It makes compiling code which uses Apple EAWT
 * on non-Mac platforms possible.  The compiled stub classes should never be
 * included in the final product.
 */

package com.apple.eawt;

/**
 * @deprecated
 */
@java.lang.Deprecated
public interface ApplicationListener extends java.util.EventListener {

  /**
   * @deprecated
   */
  @java.lang.Deprecated
  void handleAbout(ApplicationEvent event);

  /**
   * @deprecated
   */
  @java.lang.Deprecated
  void handleOpenApplication(ApplicationEvent event);

  /**
   * @deprecated
   */
  @java.lang.Deprecated
  void handleOpenFile(ApplicationEvent event);

  /**
   * @deprecated
   */
  @java.lang.Deprecated
  void handlePreferences(ApplicationEvent event);

  /**
   * @deprecated
   */
  @java.lang.Deprecated
  void handlePrintFile(ApplicationEvent event);

  /**
   * @deprecated
   */
  @java.lang.Deprecated
  void handleQuit(ApplicationEvent event);

  /**
   * @deprecated
   */
  @java.lang.Deprecated
  void handleReOpenApplication(ApplicationEvent event);
}