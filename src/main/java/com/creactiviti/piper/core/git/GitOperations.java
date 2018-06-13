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
package com.creactiviti.piper.core.git;

import java.util.List;

import com.creactiviti.piper.core.pipeline.IdentifiableResource;

public interface GitOperations {
  
  /**
   * Returns the list of HEAD files within a Git repository. 
   * 
   * @param aUrl
   *          The Git Repository URL
   * @param aSearchPath
   *          The path to limit the search to.
   * @return the list of {@link IdentifiableResource}s within the given 
   * search path of the repository
   */
  List<IdentifiableResource> getHeadFiles (String aUrl, String... aSearchPath);
  
  /**
   * 
   */
  IdentifiableResource getFile (String aUrl, String aFileId);

}
