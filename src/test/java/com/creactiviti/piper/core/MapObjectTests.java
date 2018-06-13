
package com.creactiviti.piper.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;


public class MapObjectTests {

  @Test
  public void test1 () {
    MapObject mo = new MapObject(Collections.singletonMap("hello", "world")) {};
    Assert.assertEquals("world", mo.getString("hello"));
  }
  
  @Test
  public void test2 () {
    MapObject mo = new MapObject(Collections.singletonMap("number", "5")) {};
    Assert.assertEquals(Integer.valueOf(5), mo.getInteger("number"));
  }
  
  @Test
  public void test3 () {
    MapObject mo = new MapObject(Collections.singletonMap("number", "5")) {};
    Assert.assertEquals(Double.valueOf(5), mo.getDouble("number"));
  }
  
  @Test
  public void test4 () {
    MapObject mo = new MapObject(Collections.singletonMap("number", "5")) {};
    Assert.assertEquals(Double.valueOf(3), mo.getDouble("double",3));
  }
  
  @Test
  public void test5 () {
    MapObject mo = new MapObject(Collections.singletonMap("number", "5")) {};
    Assert.assertEquals(1, mo.size());
  }

  @Test
  public void test6 () {
    MapObject mo = new MapObject(Collections.singletonMap("number", "5")) {};
    Assert.assertTrue(mo.containsKey("number"));
    Assert.assertFalse(mo.containsKey("none"));
  }
  
  @Test
  public void test7 () {
    MapObject mo = new MapObject(Collections.singletonMap("number", "5")) {};
    Assert.assertTrue(mo.containsValue("5"));
  }
  
  @Test
  public void test8 () {
    MapObject mo = new MapObject(Collections.singletonMap("hello", "world")) {};
    Assert.assertEquals("world", mo.get("hello"));
  }
  
  @Test(expected=ClassCastException.class)
  public void test9 () {
    MapObject mo = new MapObject(Collections.singletonMap("hello", "world")) {};
    Assert.assertEquals(Arrays.asList("world"), mo.getList("hello",String.class));
  }
  
  @Test
  public void test10 () {
    MapObject mo = new MapObject(Collections.singletonMap("hello", Arrays.asList("world"))) {};
    List<String> list = mo.getList("hello",String.class);
    Assert.assertEquals(Arrays.asList("world"), list);
  }
  
  @Test
  public void test11 () {
    MapObject mo = new MapObject(Collections.singletonMap("key", 1)) {};
    Assert.assertEquals("1",mo.getString("key"));
  }
  
  @Test
  public void test12 () {
    MapObject mo = new MapObject(Collections.singletonMap("key", "value")) {};
    Assert.assertEquals("value",mo.getRequiredString("key"));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void test13 () {
    MapObject mo = new MapObject(Collections.singletonMap("key", "value")) {};
    Assert.assertEquals("value",mo.getRequiredString("anotherKey"));
  }
  
  @Test
  public void test14 () {
    MapObject mo = new MapObject(Collections.singletonMap("key", "value")) {};
    Assert.assertEquals("anotherValue",mo.getString("anotherKey","anotherValue"));
  }
  
}
