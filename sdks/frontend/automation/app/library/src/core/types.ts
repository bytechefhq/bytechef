import type {components} from '../generated/openapi';

export type JsonSchema = Record<string, unknown>;

export type AppBinding = components['schemas']['AppBinding'];

export type AppApiCollection = components['schemas']['AppApiCollection'];

export type AppApiEndpoint = components['schemas']['AppApiEndpoint'];

export type AppAction = Omit<components['schemas']['AppAction'], 'inputSchema'> & {inputSchema: JsonSchema};

export type AppTool = Omit<components['schemas']['AppTool'], 'parameters'> & {parameters: JsonSchema};

export type ProxyBody = string | ArrayBuffer | ArrayBufferView | Record<string, unknown> | unknown[];

export type ProxyRequest = Omit<components['schemas']['ProxyRequest'], 'body' | 'bodyEncoding' | 'contentType'> & {
    body?: ProxyBody;
    contentType?: string;
};

export interface ProxyResult {
    body: string | null;
    bodyEncoding: 'BASE64' | 'UTF8';
    bytes(): Uint8Array;
    headers: Record<string, string[]>;
    json<T = unknown>(): T;
    status: number;
    text(): string;
}

export type WorkflowRunStatus = components['schemas']['WorkflowRunStatus'];

export type AppWorkflow = Omit<components['schemas']['AppWorkflow'], 'inputSchema' | 'outputSchema'> & {
    inputSchema: JsonSchema | null;
    outputSchema: JsonSchema | null;
};

export type WorkflowRun = components['schemas']['WorkflowRun'];
