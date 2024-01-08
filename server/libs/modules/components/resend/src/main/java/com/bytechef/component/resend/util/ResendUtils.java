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

package com.bytechef.component.resend.util;

import static com.bytechef.component.resend.constant.ResendConstants.ATTACHMENTS;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import com.bytechef.hermes.component.definition.Parameters;
import com.resend.services.emails.model.Attachment;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class ResendUtils {

    private ResendUtils() {
    }

    public static List<Attachment> getAttachments(Parameters inputParameters, ActionContext actionContext) {
        List<Attachment> attachmentList = new ArrayList<>();

        for (FileEntry fileEntry : inputParameters.getFileEntries(ATTACHMENTS, List.of())) {
            Attachment attachment = new Attachment.Builder()
                .fileName(fileEntry.getName())
                .content(Base64.getEncoder()
                    .encodeToString(
                        actionContext.file(file -> file.getStream(fileEntry)
                            .readAllBytes())))
                .build();

            attachmentList.add(attachment);
        }

        return attachmentList;
    }
}
