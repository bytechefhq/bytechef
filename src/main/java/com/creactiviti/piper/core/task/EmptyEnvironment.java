package com.creactiviti.piper.core.task;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * @author Arik Cohen
 * @since Mar, 06 2020
 */
public class EmptyEnvironment implements Environment {

  @Override
  public boolean containsProperty(String aKey) {
    return false;
  }

  @Override
  public String getProperty(String aKey) {
    return null;
  }

  @Override
  public String getProperty(String aKey, String aDefaultValue) {
    return null;
  }

  @Override
  public <T> T getProperty(String aKey, Class<T> aTargetType) {
    return null;
  }

  @Override
  public <T> T getProperty(String aKey, Class<T> aTargetType, T aDefaultValue) {
    return null;
  }

  @Override
  public String getRequiredProperty(String aKey) throws IllegalStateException {
    return null;
  }

  @Override
  public <T> T getRequiredProperty(String aKey, Class<T> aTargetType) throws IllegalStateException {
    return null;
  }

  @Override
  public String resolvePlaceholders(String aText) {
    return null;
  }

  @Override
  public String resolveRequiredPlaceholders(String aText) throws IllegalArgumentException {
    return null;
  }

  @Override
  public String[] getActiveProfiles() {
    return new String[0];
  }

  @Override
  public String[] getDefaultProfiles() {
    return new String[0];
  }

  @Override
  public boolean acceptsProfiles(String... aProfiles) {
    return false;
  }

  @Override
  public boolean acceptsProfiles(Profiles aProfiles) {
    return false;
  }

}
