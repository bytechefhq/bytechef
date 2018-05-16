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
package com.creactiviti.piper.core.context;

import java.util.List;


/**
 * <p>Stores context information for a job or task
 * objects.</p>
 * 
 * <p>{@link Context} instances are used to evaluate
 * pipeline tasks before they are executed.</p>
 * 
 * @author Arik Cohen
 * @since Mar 2017
 */
public interface ContextRepository<T extends Context> {

  T push (String aStackId, T aContext);
  
  T peek (String aStackId);
  
  List<T> getStack (String aStackId);
  
}
