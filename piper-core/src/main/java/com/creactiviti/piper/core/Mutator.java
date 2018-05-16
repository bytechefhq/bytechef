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

public interface Mutator {

  /**
   * Creates an association between the given key
   * and the given value.
   * 
   * @param aKey
   *          The key to associate with the value
   * @param aValue
   *          The value to associate with the key (can be <code>null</code>).
   */
  void set (String aKey, Object aValue);

  /**
   * Creates an association between the given key
   * and the given value ONLY if the key is not already 
   * associated with a value or if the value associated 
   * with the key is <code>null</code>.
   * 
   * @param aKey
   *          The key to associate with the value
   * @param aValue
   *          The value to associate with the key (can be <code>null</code>).
   */
  void setIfNull (String aKey, Object aValue);
  
  /**
   * Increment the specified key counter by 1.
   * 
   * @param aKey the name of the counter
   * @return the new counter value
   */
  long increment (String aKey);
  
}
