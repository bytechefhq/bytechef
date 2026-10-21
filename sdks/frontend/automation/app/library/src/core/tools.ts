import type {components} from '../generated/openapi';
import type {CallOptions} from './connections';
import {BytechefAppError} from './errors';
import {abortError, type Requester, untilAborted} from './request';
import {expectArray, expectObject} from './response';
import type {AppTool, JsonSchema} from './types';

export const TOOL_LISTING_TTL_MS = 60_000;

type RawTool = components['schemas']['AppTool'];

const ANY_OBJECT: JsonSchema = {additionalProperties: true, type: 'object'};

const TOOLS_PATH = '/tools';

function parseParameters(parameters: string): JsonSchema {
    try {
        const parsed: unknown = JSON.parse(parameters);

        return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)
            ? (parsed as JsonSchema)
            : {...ANY_OBJECT};
    } catch {
        return {...ANY_OBJECT};
    }
}

export function createToolsApi(request: Requester, now: () => number = () => Date.now()) {
    let cache: {fetchedAt: number; tools: AppTool[]} | undefined;
    let sharedListing: Promise<AppTool[]> | undefined;

    async function fetchListing(signal?: AbortSignal): Promise<AppTool[]> {
        const tools = expectArray<RawTool>(await request({method: 'GET', path: TOOLS_PATH, signal}), TOOLS_PATH).map(
            (tool) => ({...tool, parameters: parseParameters(tool.parameters)})
        );

        cache = {fetchedAt: now(), tools};

        return tools;
    }

    function refreshListing(signal?: AbortSignal): Promise<AppTool[]> {
        if (signal?.aborted) {
            return Promise.reject(abortError(signal));
        }

        sharedListing ??= fetchListing().finally(() => {
            sharedListing = undefined;
        });

        return signal ? untilAborted(sharedListing, signal) : sharedListing;
    }

    function cachedListing(signal?: AbortSignal): Promise<AppTool[]> {
        if (cache && now() - cache.fetchedAt < TOOL_LISTING_TTL_MS) {
            return Promise.resolve(cache.tools);
        }

        return refreshListing(signal);
    }

    async function execute<T = unknown>(
        bindingKey: string,
        toolName: string,
        input: Record<string, unknown>,
        options: CallOptions = {}
    ): Promise<T> {
        const path = `/connections/${encodeURIComponent(bindingKey)}/tools/${encodeURIComponent(toolName)}`;
        const data = expectObject<{result: T}>(
            await request({body: {input}, method: 'POST', path, signal: options.signal}),
            path
        );

        return data.result;
    }

    return {
        execute,

        async executeByName<T = unknown>(
            flatName: string,
            input: Record<string, unknown>,
            options: CallOptions = {}
        ): Promise<T> {
            let tool = (await cachedListing(options.signal)).find((candidate) => candidate.name === flatName);

            if (!tool) {
                tool = (await refreshListing(options.signal)).find((candidate) => candidate.name === flatName);
            }

            if (!tool) {
                throw new BytechefAppError('APP_TOOL_NOT_FOUND', `No tool named '${flatName}' is allowed for this app`);
            }

            return execute<T>(tool.bindingKey, tool.toolName, input, options);
        },

        async list(options: CallOptions = {}): Promise<AppTool[]> {
            return [...(await fetchListing(options.signal))];
        },
    };
}
