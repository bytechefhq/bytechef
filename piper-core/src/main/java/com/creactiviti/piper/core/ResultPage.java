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
package com.creactiviti.piper.core;

import java.util.Collections;
import java.util.List;

public class ResultPage<T> extends MapObject implements Page<T> {

  private final Class<T> elementType;
  
  public ResultPage(Class<T> aElementType) {
    super(Collections.EMPTY_MAP);
    elementType = aElementType;
  }

  @Override
  public List<T> getItems () {
    List<T> list = getList("items", elementType);
    return list!=null?list:Collections.EMPTY_LIST;
  }

  public void setItems (List<T> aItems) {
    set("items", aItems);
    set("size",aItems.size());
  }
  
  @Override
  public int getSize() {
    return getInteger("size", 0);
  }

  @Override
  public int getTotalItems() {
    return getInteger("totalItems", 0);
  }

  public void setTotalItems (int aTotalElements) {
    set("totalItems", aTotalElements);
  }
  
  @Override
  public int getTotalPages() {
    return getInteger("totalPages", 0);
  }
  
  public void setTotalPages(int aTotalPages) {
    set("totalPages", aTotalPages);
  }

  @Override
  public int getNumber() {
    return getInteger("number", 0);
  }
  
  public void setNumber (int aNumber) {
    set("number", aNumber);
  }

}
