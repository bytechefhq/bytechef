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

package com.bytechef.component.resend;

import static com.bytechef.component.resend.constant.ResendConstants.ATTACHMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.resend.util.ResendUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.Parameters;
import com.resend.services.emails.model.Attachment;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class ResendUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testGetAttachmentsForNoneFileEntry() {
        when(mockedParameters.getFileEntries(ATTACHMENTS, List.of())).thenReturn(List.of());

        List<Attachment> attachments = ResendUtils.getAttachments(mockedParameters, mockedContext);

        assertEquals(0, attachments.size());
        //TODO write test for getAttachments with file entries
    }
}
