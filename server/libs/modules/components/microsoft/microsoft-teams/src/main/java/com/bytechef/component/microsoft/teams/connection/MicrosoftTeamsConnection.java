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

package com.bytechef.component.microsoft.teams.connection;

import static com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

import com.bytechef.microsoft.commons.MicrosoftConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftTeamsConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = MicrosoftConnection.createConnection(
        (connection, context) -> {
            Map<String, Boolean> map = new HashMap<>();

            map.put("AgentIdUser.ReadWrite.All", false);
            map.put("AgentIdUser.ReadWrite.IdentityParentedBy", false);
            map.put("Channel.Create", true);
            map.put("Channel.Delete.All", false);
            map.put("Channel.ReadBasic.All", true);
            map.put("ChannelMessage.Send", true);
            map.put("ChannelSettings.Read.All", false);
            map.put("ChannelSettings.Read.Group", false);
            map.put("ChannelSettings.ReadWrite.All", false);
            map.put("ChannelSettings.ReadWrite.Group", false);
            map.put("Chat.ReadWrite", true);
            map.put("Directory.Read.All", false);
            map.put("Directory.ReadWrite.All", false);
            map.put("Group.Read.All", false);
            map.put("Group.ReadWrite.All", false);
            map.put("Team.Create", false);
            map.put("Team.ReadBasic.All", true);
            map.put("TeamMember.Read.All", false);
            map.put("TeamMember.Read.Group", false);
            map.put("TeamMember.ReadWrite.All", false);
            map.put("TeamMember.ReadWriteNonOwnerRole.All", false);
            map.put("TeamSettings.Read.All", false);
            map.put("TeamSettings.Read.Group", false);
            map.put("TeamSettings.ReadWrite.All", false);
            map.put("TeamSettings.ReadWrite.Group", false);
            map.put("Teamwork.Migrate.All", false);
            map.put("TeamsApp.Read.Group", false);
            map.put("TeamsAppInstallation.ManageSelectedForTeam", false);
            map.put("TeamsAppInstallation.ManageSelectedForTeam.All", false);
            map.put("TeamsAppInstallation.Read.All", false);
            map.put("TeamsAppInstallation.Read.Group", false);
            map.put("TeamsAppInstallation.ReadForTeam", false);
            map.put("TeamsAppInstallation.ReadForTeam.All", false);
            map.put("TeamsAppInstallation.ReadForUser", false);
            map.put("TeamsAppInstallation.ReadWriteAndConsentForTeam", false);
            map.put("TeamsAppInstallation.ReadWriteAndConsentForTeam.All", false);
            map.put("TeamsAppInstallation.ReadWriteAndConsentSelfForTeam", false);
            map.put("TeamsAppInstallation.ReadWriteAndConsentSelfForTeam.All", false);
            map.put("TeamsAppInstallation.ReadWriteForTeam", false);
            map.put("TeamsAppInstallation.ReadWriteForTeam.All", false);
            map.put("TeamsAppInstallation.ReadWriteSelfForTeam", false);
            map.put("TeamsAppInstallation.ReadWriteSelfForTeam.All", false);
            map.put("User.Read.All", false);
            map.put("User.ReadWrite.All", false);
            map.put("offline_access", true);

            return map;
        });

    private MicrosoftTeamsConnection() {
    }
}
