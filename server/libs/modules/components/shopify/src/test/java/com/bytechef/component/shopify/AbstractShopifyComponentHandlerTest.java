package com.bytechef.component.shopify;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * Provides the base test implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractShopifyComponentHandlerTest {
  @Test
  public void testGetDefinition() {
    JsonFileAssert.assertEquals("definition/shopify_v1.json", new ShopifyComponentHandler().getDefinition());
  }
}
