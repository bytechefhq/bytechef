package com.bytechef.component.canva;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * Provides the base test implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractCanvaComponentHandlerTest {
  @Test
  public void testGetDefinition() {
    JsonFileAssert.assertEquals("definition/canva_v1.json", new CanvaComponentHandler().getDefinition());
  }
}
