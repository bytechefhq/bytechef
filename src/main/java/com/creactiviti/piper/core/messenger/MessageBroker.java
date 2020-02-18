/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creactiviti.piper.core.messenger;


/**
 * <p>Abstraction for sending messages between the various componentes of the application. 
 * Implementations are responsible for the guranteed delivery of the message.</p>
 * 
 * @author Arik Cohen
 * @since Jun 18, 2016
 */
public interface MessageBroker {

  /**
   * 
   * @param aRoutingKey
   *          a string representaiton used for routing the message 
   *          to the appropriate destination.
   * @param aMessage
   *          The message to send.
   */
  void send (String aRoutingKey, Object aMessage);
  
}
