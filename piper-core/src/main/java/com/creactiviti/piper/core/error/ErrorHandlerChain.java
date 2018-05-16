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
package com.creactiviti.piper.core.error;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * 
 * @author Arik Cohen
 * @since Apr 10, 2017
 */
public class ErrorHandlerChain implements ErrorHandler<Errorable> {

  private final List<ErrorHandler> handlers;

  public ErrorHandlerChain(List<ErrorHandler> aHandlers) {
    Assert.notNull(aHandlers,"list of handlers must not be null");
    handlers = aHandlers;
  }
  
  @Override
  public void handle(Errorable aErrorable) {
    for(ErrorHandler handler : handlers) {
      Method method = BeanUtils.findDeclaredMethodWithMinimalParameters(handler.getClass(), "handle");
      if(method.getParameters()[0].getType().isAssignableFrom(aErrorable.getClass())) {
        handler.handle(aErrorable);
      }
    }
  }

}
