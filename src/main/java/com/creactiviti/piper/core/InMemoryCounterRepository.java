package com.creactiviti.piper.core;

import java.util.HashMap;
import java.util.Map;

import com.creactiviti.piper.core.task.CounterRepository;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
class InMemoryCounterRepository implements CounterRepository {
  
  private final Map<String, Long> counters = new HashMap<>();

  @Override
  public void set(String aCounterName, long aValue) {
    counters.put(aCounterName, aValue);
  }

  @Override
  public long decrement(String aCounterName) {
    Long value = counters.getOrDefault(aCounterName,0L) - 1;
    counters.put(aCounterName, value);
    return value;
  }

  @Override
  public void delete (String aCounterName) {
    counters.remove(aCounterName);
  }

}
