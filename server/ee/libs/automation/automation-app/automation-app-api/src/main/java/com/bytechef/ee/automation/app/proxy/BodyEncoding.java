/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

/**
 * How a proxied HTTP body is carried in JSON: {@link #UTF8} for text, {@link #BASE64} for arbitrary bytes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum BodyEncoding {

    UTF8,
    BASE64
}
