commit f264057676b8e5eab8f1d5d4d077a009bc49ebfa
Author: Ivica Cardic <ivica.cardic@gmail.com>
Date:   Sat Sep 5 18:46:29 2026 +0200

    - server - Fix final-review findings for Data Sync (visibility gate, connections block, dead validation)
    
    Closes an authorization hole where updateDataSyncElement had no registered
    ResourceVisibilityProvider, so a workspace editor without visibility on a
    withheld sync could still rewrite its source/destination via element id.
    Adds DataSyncElementVisibilityProvider, mirroring the sibling AI Agent
    feature's element provider.
    
    Stops the workflow generator from emitting an unfillable connections block
    for the dataStreamProcessor element (which never has a connection) and for
    source/destination elements that have no connectionId configured yet;
    the golden snapshots that change with it (the manual and schedule shapes)
    are regenerated in this stretch's trailing generated commit.
    
    Enforces DataSync.MAX_TITLE_LENGTH/MAX_DESCRIPTION_LENGTH in the setters so
    an over-length value fails with a clear error instead of a raw database
    constraint violation.
    
    Strengthens the generator's determinism test with a scrambled-element-order
    case that pins clusterElements key order directly, and extends the delete
    integration test to assert the draft workflow row is actually gone rather
    than only the FK-cascaded element rows.

diff --git a/server/libs/automation/automation-data-sync/automation-data-sync-api/src/test/java/com/bytechef/automation/datasync/domain/DataSyncTest.java b/server/libs/automation/automation-data-sync/automation-data-sync-api/src/test/java/com/bytechef/automation/datasync/domain/DataSyncTest.java
new file mode 100644
index 00000000000..92b9e92135d
--- /dev/null
+++ b/server/libs/automation/automation-data-sync/automation-data-sync-api/src/test/java/com/bytechef/automation/datasync/domain/DataSyncTest.java
@@ -0,0 +1,82 @@
+/*
+ * Copyright 2025 ByteChef
+ *
+ * Licensed under the Apache License, Version 2.0 (the "License");
+ * you may not use this file except in compliance with the License.
+ * You may obtain a copy of the License at
+ *
+ *      https://www.apache.org/licenses/LICENSE-2.0
+ *
+ * Unless required by applicable law or agreed to in writing, software
+ * distributed under the License is distributed on an "AS IS" BASIS,
+ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
+ * See the License for the specific language governing permissions and
+ * limitations under the License.
+ */
+
+package com.bytechef.automation.datasync.domain;
+
+import static org.assertj.core.api.Assertions.assertThat;
+import static org.assertj.core.api.Assertions.assertThatThrownBy;
+
+import java.util.UUID;
+import org.junit.jupiter.api.Test;
+
+/**
+ * Pins {@link DataSync#MAX_TITLE_LENGTH} and {@link DataSync#MAX_DESCRIPTION_LENGTH} as enforced limits, not dead
+ * constants: without a setter check an over-length value would surface only as a raw database constraint error.
+ *
+ * @author Ivica Cardic
+ */
+class DataSyncTest {
+
+    @Test
+    void testSetTitleRejectsOverLongValue() {
+        DataSync dataSync = new DataSync();
+
+        String tooLong = "a".repeat(DataSync.MAX_TITLE_LENGTH + 1);
+
+        assertThatThrownBy(() -> dataSync.setTitle(tooLong)).isInstanceOf(IllegalArgumentException.class);
+
+        String maxLength = "a".repeat(DataSync.MAX_TITLE_LENGTH);
+
+        dataSync.setTitle(maxLength);
+
+        assertThat(dataSync.getTitle()).isEqualTo(maxLength);
+    }
+
+    @Test
+    void testSetDescriptionRejectsOverLongValue() {
+        DataSync dataSync = new DataSync();
+
+        String tooLong = "a".repeat(DataSync.MAX_DESCRIPTION_LENGTH + 1);
+
+        assertThatThrownBy(() -> dataSync.setDescription(tooLong)).isInstanceOf(IllegalArgumentException.class);
+
+        String maxLength = "a".repeat(DataSync.MAX_DESCRIPTION_LENGTH);
+
+        dataSync.setDescription(maxLength);
+
+        assertThat(dataSync.getDescription()).isEqualTo(maxLength);
+    }
+
+    @Test
+    void testProjectWorkflowUuidRoundTrip() {
+        DataSync dataSync = new DataSync();
+
+        UUID projectWorkflowUuid = UUID.randomUUID();
+
+        dataSync.setProjectWorkflowUuid(projectWorkflowUuid);
+
+        assertThat(dataSync.getProjectWorkflowUuid()).isEqualTo(projectWorkflowUuid);
+    }
+
+    @Test
+    void testSetDescriptionAcceptsNull() {
+        DataSync dataSync = new DataSync();
+
+        dataSync.setDescription(null);
+
+        assertThat(dataSync.getDescription()).isNull();
+    }
+}
