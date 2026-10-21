import {existsSync, readFileSync} from 'node:fs';
import {parseEnv} from 'node:util';

import {BytechefAppError, createBytechefClient} from '@bytechef/app';

const ENV_FILE = '.env.local';

function fail(message) {
    process.stderr.write(`${message}\n`);
    process.exit(1);
}

function flatToolName(bindingKey, toolName) {
    const key = bindingKey.replace(/[^A-Za-z0-9]+/g, '_').toUpperCase();
    let tool = '';

    for (let index = 0; index < toolName.length; index++) {
        const character = toolName.charAt(index);
        const upperCase = character.toUpperCase();

        if (index > 0 && /\p{Uppercase}/u.test(character)) {
            tool += '_';
        }

        tool += upperCase.length === 1 ? upperCase : character;
    }

    return `${key}__${tool}`;
}

function describeError(error) {
    if (error instanceof BytechefAppError) {
        return error.code;
    }

    return error instanceof Error ? error.message : String(error);
}

function loadEnvFile() {
    if (!existsSync(ENV_FILE)) {
        process.stderr.write(
            `No ${ENV_FILE} here; BYTECHEF_URL and BYTECHEF_APP_KEY come from the environment only.\n`
        );

        return;
    }

    for (const [name, value] of Object.entries(parseEnv(readFileSync(ENV_FILE, 'utf8')))) {
        if (process.env[name] === undefined) {
            process.env[name] = value;
        }
    }
}

async function listActions(client) {
    try {
        return {actionSchemas: true, actions: await client.actions.list(), unavailableReason: null};
    } catch (error) {
        return {actionSchemas: false, actions: [], unavailableReason: describeError(error)};
    }
}

loadEnvFile();

try {
    const client = createBytechefClient();
    const [bindings, listed, tools, workflows, apiCollections] = await Promise.all([
        client.connections.list(),
        listActions(client),
        client.tools.list(),
        client.workflows.list(),
        client.api.list(),
    ]);

    const status = {
        actionSchemas: listed.actionSchemas,
        apiCollections: apiCollections.map((collection) => ({
            available: collection.available,
            endpoints: collection.endpoints.map((endpoint) => `${endpoint.httpMethod} ${endpoint.path ?? ''}`),
            name: collection.name,
            path: [`v${collection.collectionVersion}`, collection.contextPath]
                .filter((segment) => !!segment)
                .map((segment) => segment.replace(/^\/+|\/+$/g, ''))
                .join('/'),
        })),
        bindings: bindings.map((binding) => ({
            actions: binding.actionNames.map((name) => {
                const action = listed.actions.find(
                    (candidate) => candidate.bindingKey === binding.bindingKey && candidate.actionName === name
                );

                return {description: action?.description ?? null, inputSchema: action?.inputSchema ?? null, name};
            }),
            bindingKey: binding.bindingKey,
            componentName: binding.componentName,
            componentVersion: binding.componentVersion ?? null,
            proxy: {enabled: binding.proxyEnabled, methods: [...binding.proxyAllowedMethods]},
            tools: binding.toolNames.map((toolName) => {
                const tool = tools.find(
                    (candidate) => candidate.bindingKey === binding.bindingKey && candidate.toolName === toolName
                );

                return tool
                    ? {description: tool.description ?? null, name: tool.name, parameters: tool.parameters, toolName}
                    : {description: null, name: flatToolName(binding.bindingKey, toolName), parameters: null, toolName};
            }),
        })),
        bytechefUrl: process.env.BYTECHEF_URL,
        workflows,
    };

    if (listed.unavailableReason !== null) {
        process.stderr.write(`Action schemas unavailable: ${listed.unavailableReason}\n`);
    }

    process.stdout.write(`${JSON.stringify(status, null, 2)}\n`);
} catch (error) {
    if (error instanceof BytechefAppError) {
        fail(`${error.code}: ${error.message}`);
    }

    if (error instanceof Error) {
        fail(error.message);
    }

    throw error;
}
