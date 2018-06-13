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

import java.util.Map;

import com.creactiviti.piper.core.MapObject;

/**
 * 
 * @author Arik Cohen
 * @since Apr 10, 2017
 */
public class ErrorObject extends MapObject implements Error {

  public ErrorObject () {
  }
  
  public ErrorObject (Map<String,Object> aSource) {
    super(aSource);
  }
  
  public ErrorObject (String aMessage, String[] aStackTrace) {
    set("message", aMessage);
    set("stackTrace", aStackTrace);
  } 
  
  @Override
  public String getMessage() {
    return getString("message");
  }

  @Override
  public String[] getStackTrace() {
    return getArray("stackTrace", String.class);
  }

}
