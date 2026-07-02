import {useCallback} from 'react';

import {ApiFetch} from './types';

export type ExecuteActionFunction = (
    componentName: string,
    componentVersion: number,
    actionName: string,
    input: Record<string, unknown>
) => Promise<unknown[]>;

/**
 * Returns a function the embedding app's field-mapping callbacks use to fetch object types / integration fields. It
 * proxies to the embedded generic action endpoint, which runs the named component action against the connected
 * account's live credentials. The integration instance id is bound here (sent as `X-Instance-Id`) so callbacks never
 * thread it and cannot target another user's instance. Returns the action's `result` array (or `[]`).
 */
export default function useExecuteAction(
    apiFetch: ApiFetch | undefined,
    externalUserId: string | undefined,
    integrationInstanceId: number | undefined
): ExecuteActionFunction {
    return useCallback(
        async (componentName, componentVersion, actionName, input) => {
            if (!apiFetch || !externalUserId || !integrationInstanceId) {
                return [];
            }

            try {
                const response = await apiFetch<{result?: unknown[]}>(
                    `/api/embedded/v1/${externalUserId}/components/${componentName}/versions/${componentVersion}/actions/${actionName}`,
                    {
                        body: {input},
                        headers: {'X-Instance-Id': String(integrationInstanceId)},
                        method: 'POST',
                    }
                );

                return response?.result ?? [];
            } catch (error: unknown) {
                console.error('Failed to execute action:', (error as Error).message);

                return [];
            }
        },
        [apiFetch, externalUserId, integrationInstanceId]
    );
}
