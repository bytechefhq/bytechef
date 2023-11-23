/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.dropbox.action;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.bytechef.hermes.component.definition.ActionDefinition;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
class DropboxListFolderActionTest extends DropboxActionTestAbstract {

    @Test
    void testPerform() throws DbxException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            DropboxListFolderAction.perform(
                parameterMap, parameterMap, Mockito.mock(ActionDefinition.ActionContext.class));
        });

        then(filesRequests).should(times(1))
            .listFolder(stringArgumentCaptorA.capture());

        Assertions.assertEquals(SOURCE_STUB, stringArgumentCaptorA.getValue());
    }

    @Test
    @SuppressFBWarnings
    void testListFolderResult() {
        ListFolderResult listFolderResult = Mockito.mock(ListFolderResult.class);

        new DropboxListFolderAction.ListFolderResult(listFolderResult);

        then(listFolderResult).should(times(1))
            .getEntries();
        then(listFolderResult).should(times(1))
            .getCursor();
        then(listFolderResult).should(times(1))
            .getHasMore();
    }

    @Test
    @SuppressFBWarnings
    void testMetadata() {
        Metadata metadata = Mockito.mock(Metadata.class);

        new DropboxListFolderAction.Metadata(metadata);

        then(metadata).should(times(1))
            .getName();
        then(metadata).should(times(1))
            .getPathLower();
        then(metadata).should(times(1))
            .getPathDisplay();
        then(metadata).should(times(1))
            .getParentSharedFolderId();
        then(metadata).should(times(1))
            .getPreviewUrl();
    }
}
