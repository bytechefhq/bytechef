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
package com.creactiviti.piper.core.event;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import com.creactiviti.piper.core.Accessor;
import com.creactiviti.piper.core.DSL;
import com.creactiviti.piper.core.MapObject;
import com.creactiviti.piper.core.uuid.UUIDGenerator;
import com.google.common.collect.ImmutableMap;

/**
 * 
 * @author Arik Cohen
 * @since Apr 8, 2017
 */
public class PiperEvent extends MapObject  implements Accessor {

  public PiperEvent() {
    super();
  }
  
  public PiperEvent(Map<String,Object> aSource) {
    super(new MapObject(aSource));
  }
  
  public String getType () {
    return getRequiredString(DSL.TYPE);
  }
  
  public Date getCreateTime () {
    return getDate(DSL.CREATE_TIME);
  }

  public static PiperEvent of (String aType) {
    return of (aType, Collections.EMPTY_MAP);
  }
  
  public static PiperEvent of (String aType, String aKey, Object aValue) {
    Assert.notNull(aKey,"key must not be null");
    Assert.notNull(aValue,"value for " + aKey + " must not be null");
    return of(aType,ImmutableMap.of(aKey, aValue));
  }
  
  public static PiperEvent of (String aType, String aKey1, Object aValue1, String aKey2, Object aValue2) {
    Assert.notNull(aKey1,"key must not be null");
    Assert.notNull(aValue1,"value for " + aKey1 + " must not be null");
    Assert.notNull(aKey2,"key must not be null");
    Assert.notNull(aValue2,"value for " + aKey2 + " must not be null");
    return of(aType,ImmutableMap.of(aKey1, aValue1,aKey2,aValue2));
  }
  
  public static PiperEvent of (String aType, Map<String, Object> aProperties) {
    Assert.notNull(aType,"event type must not be null");
    Map<String,Object> source = new HashMap<>(ImmutableMap.of("id",UUIDGenerator.generate(),"type", aType,"createTime",new Date()));
    source.putAll(aProperties);
    return new PiperEvent(source);
  }
  
}
