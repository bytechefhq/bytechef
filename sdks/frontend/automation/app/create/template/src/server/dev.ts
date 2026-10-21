import type {AppAction, BytechefClient} from '@bytechef/app';
import {
    callApiHandler,
    callApiInput,
    executeActionHandler,
    executeActionInput,
    type JsonSerializable,
} from '@bytechef/app/tanstack-start';
import {createServerFn} from '@tanstack/react-start';

import {bytechef} from './bytechef';
import {buildOverview, type AppOverview} from './overview';

async function listActions(client: BytechefClient): Promise<{actionSchemas: boolean; actions: AppAction[]}> {
    try {
        return {actionSchemas: true, actions: await client.actions.list()};
    } catch {
        return {actionSchemas: false, actions: []};
    }
}

export const getAppOverview = createServerFn({method: 'GET'})
    .middleware([bytechef])
    .handler(async ({context}): Promise<JsonSerializable<AppOverview>> => {
        const client = context.bytechef;
        const [bindings, listed, tools, workflows, apiCollections] = await Promise.all([
            client.connections.list(),
            listActions(client),
            client.tools.list(),
            client.workflows.list(),
            client.api.list(),
        ]);

        return buildOverview(
            bindings,
            listed.actions,
            listed.actionSchemas,
            tools,
            workflows,
            apiCollections
        ) as JsonSerializable<AppOverview>;
    });

export const tryAction = createServerFn({method: 'POST'})
    .middleware([bytechef])
    .validator(executeActionInput)
    .handler(executeActionHandler);

export const tryApi = createServerFn({method: 'POST'})
    .middleware([bytechef])
    .validator(callApiInput)
    .handler(callApiHandler);
