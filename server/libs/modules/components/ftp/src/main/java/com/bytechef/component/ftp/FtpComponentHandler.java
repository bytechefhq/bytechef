/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ftp;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.ftp.action.FtpDeleteAction;
import com.bytechef.component.ftp.action.FtpDownloadFileAction;
import com.bytechef.component.ftp.action.FtpListAction;
import com.bytechef.component.ftp.action.FtpRenameAction;
import com.bytechef.component.ftp.action.FtpUploadFileAction;
import com.bytechef.component.ftp.connection.FtpConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class FtpComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("ftp")
        .title("FTP")
        .description(
            "FTP (File Transfer Protocol) is a standard network protocol for transferring files between a client " +
                "and a server. It allows uploading, downloading, and managing files on remote servers.")
        .icon("path:assets/ftp.svg")
        .categories(ComponentCategory.FILE_STORAGE, ComponentCategory.HELPERS)
        .connection(FtpConnection.CONNECTION_DEFINITION)
        .actions(
            FtpUploadFileAction.ACTION_DEFINITION,
            FtpDownloadFileAction.ACTION_DEFINITION,
            FtpListAction.ACTION_DEFINITION,
            FtpDeleteAction.ACTION_DEFINITION,
            FtpRenameAction.ACTION_DEFINITION)
        .clusterElements(
            tool(FtpUploadFileAction.ACTION_DEFINITION),
            tool(FtpDownloadFileAction.ACTION_DEFINITION),
            tool(FtpListAction.ACTION_DEFINITION),
            tool(FtpDeleteAction.ACTION_DEFINITION),
            tool(FtpRenameAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
