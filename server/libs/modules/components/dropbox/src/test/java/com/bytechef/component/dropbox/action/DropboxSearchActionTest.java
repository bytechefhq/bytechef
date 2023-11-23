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

import static com.bytechef.component.dropbox.constant.DropboxConstants.SEARCH_STRING;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.bytechef.hermes.component.definition.ActionDefinition;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.HighlightSpan;
import com.dropbox.core.v2.files.SearchMatchV2;
import com.dropbox.core.v2.files.SearchV2Result;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Mario Cvjetojevic
 */
class DropboxSearchActionTest extends DropboxActionTestAbstract {

    @Test
    void testPerform() throws DbxException {
        Mockito.when(parameterMap.getRequiredString(SEARCH_STRING))
            .thenReturn(SOURCE_STUB);

        Assertions.assertThrows(NullPointerException.class, () -> {
            DropboxSearchAction.perform(
                parameterMap, parameterMap, Mockito.mock(ActionDefinition.ActionContext.class));
        });

        then(filesRequests).should(times(1))
            .searchV2(stringArgumentCaptorA.capture());

        Assertions.assertEquals(SOURCE_STUB, stringArgumentCaptorA.getValue());
    }

    @Test
    @SuppressFBWarnings
    void testSearchV2Result() {
        SearchV2Result searchV2Result = Mockito.mock(SearchV2Result.class);

        new DropboxSearchAction.SearchV2Result(searchV2Result);

        then(searchV2Result).should(times(1))
            .getMatches();
        then(searchV2Result).should(times(1))
            .getCursor();
        then(searchV2Result).should(times(1))
            .getHasMore();
    }

    @Test
    @SuppressFBWarnings
    void testSearchMatchV2() {
        SearchMatchV2 searchMatchV2 = Mockito.mock(SearchMatchV2.class);

        new DropboxSearchAction.SearchMatchV2(searchMatchV2);

        then(searchMatchV2).should(times(1))
            .getHighlightSpans();
    }

    @Test
    @SuppressFBWarnings
    void testHighlightSpan() {
        HighlightSpan highlightSpan = Mockito.mock(HighlightSpan.class);

        new DropboxSearchAction.HighlightSpan(highlightSpan);

        then(highlightSpan).should(times(1))
            .getHighlightStr();
        then(highlightSpan).should(times(1))
            .getIsHighlighted();
    }
}
