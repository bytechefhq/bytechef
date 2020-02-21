package com.creactiviti.piper.core;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.springframework.util.Assert;

import com.creactiviti.piper.core.context.Context;
import com.creactiviti.piper.core.context.ContextRepository;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
class InMemoryContextRepository implements ContextRepository {
  
  private final Map<String, Deque<Context>> contexts = new HashMap<>();

  @Override
  public void push (String aStackId, Context aContext) {
    Deque<Context> stack = contexts.get(aStackId);
    if(stack == null) {
      stack = new LinkedList<>();
      contexts.put(aStackId, stack);
    }
    stack.push(aContext);
  }

  @Override
  public Context peek (String aStackId) {
    Deque<Context> linkedList = contexts.get(aStackId);
    Assert.notNull(linkedList,"unknown stack: " + aStackId);
    return linkedList.peek();
  }

}
