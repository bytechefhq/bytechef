import {test as base} from '@playwright/test';

import {deleteAiHubChatThroughApi, trackCreatedAiHubChats} from '../utils/aiHubUtils';

export type AiHubChatsFixturesType = {
    aiHubChatCleanup: void;
};

/**
 * Deletes every AI Hub chat the test's page created through `createAiHubChat`, once the test ends.
 */
export const aiHubChatsTest = base.extend<AiHubChatsFixturesType>({
    aiHubChatCleanup: [
        async ({page}, use) => {
            const {chats} = trackCreatedAiHubChats(page);

            await use();

            for (const chat of chats) {
                await deleteAiHubChatThroughApi(page, chat);
            }
        },
        {auto: true},
    ],
});
