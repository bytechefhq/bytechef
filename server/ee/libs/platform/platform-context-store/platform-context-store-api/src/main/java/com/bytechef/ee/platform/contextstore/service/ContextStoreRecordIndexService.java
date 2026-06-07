/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecordIndex;

/**
 * CRUD service for {@link ContextStoreRecordIndex}. Index rows are sidecar entries rebuilt on every record upsert via
 * delete-then-insert; consumers (e.g. the sync writer) call {@link #deleteAllByRecordId} before re-inserting fresh rows
 * via {@link #save}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreRecordIndexService {

    ContextStoreRecordIndex save(ContextStoreRecordIndex index);

    void deleteAllByRecordId(Long recordId);
}
