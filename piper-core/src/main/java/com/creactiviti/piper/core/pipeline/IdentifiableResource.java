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
package com.creactiviti.piper.core.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.core.io.Resource;

public class IdentifiableResource implements Resource  {

  private final String id;
  private final Resource resource;
  
  public IdentifiableResource (String aId, Resource aResource) {
    id = aId;
    resource = aResource;
  }
  
  public String getId() {
    return id;
  }
  
  @Override
  public InputStream getInputStream() throws IOException {
    return resource.getInputStream();
  }
  
  @Override
  public boolean exists() {
    return resource.exists();
  }
  
  @Override
  public boolean isReadable() {
    return resource.isReadable();
  }
  
  @Override
  public boolean isOpen() {
    return resource.isOpen();
  }
  
  @Override
  public URL getURL() throws IOException {
    return resource.getURL();
  }
  
  @Override
  public URI getURI() throws IOException {
    return resource.getURI();
  }
  
  @Override
  public File getFile() throws IOException {
    return resource.getFile();
  }
  
  @Override
  public long contentLength() throws IOException {
    return resource.contentLength();
  }
  
  @Override
  public long lastModified() throws IOException {
    return resource.lastModified();
  }
  
  @Override
  public Resource createRelative(String aRelativePath) throws IOException {
    return resource.createRelative(aRelativePath);
  }
  
  @Override
  public String getFilename() {
    return resource.getFilename();
  }
  
  @Override
  public String getDescription() {
    return resource.getDescription();
  }
  
    
}
