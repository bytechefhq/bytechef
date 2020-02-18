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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * a simple, non-thread-safe implementation of the 
 * {@link Messenger} interface. Useful for testing.
 * 
 * @author Arik Cohen
 * @since Jul 10, 2016
 */
public class SynchMessageBroker implements MessageBroker {

  private Map<String, List<Receiver>> listeners = new HashMap<> ();

  @Override
  public void send (String aRoutingKey, Object aMessage) {
    List<Receiver> list = listeners.get(aRoutingKey);
    Assert.isTrue(list!=null&&list.size()>0,"no listeners subscribed for: " + aRoutingKey);
    for(Receiver receiver : list) {
      receiver.receive(aMessage);
    }
  }

  public void receive (String aRoutingKey, Receiver aReceiver) {
    List<Receiver> list = listeners.get(aRoutingKey);
    if(list == null) {
      list = new ArrayList<>();
      listeners.put(aRoutingKey, list);
    }
    list.add(aReceiver);
  }
  
  public static interface Receiver {
    
    void receive (Object aMessage);
    
  }

}
