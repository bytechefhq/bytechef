/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.dto;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record ContextStoreSearchResult(List<ContextStoreRecord> items, @Nullable String nextCursor) {

    public ContextStoreSearchResult {
        items = List.copyOf(items);
    }
}
