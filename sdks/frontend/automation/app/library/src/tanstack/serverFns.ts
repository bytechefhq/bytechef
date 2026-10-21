import {createServerFn} from '@tanstack/react-start';

import {
    callApiHandler,
    executeActionHandler,
    executeToolByNameHandler,
    executeToolHandler,
    executeWorkflowHandler,
    getWorkflowRunHandler,
    listApiCollectionsHandler,
    listConnectionsHandler,
    listToolsHandler,
    listWorkflowsHandler,
    proxyHandler,
} from './handlers';
import {bytechefMiddleware} from './middleware';
import {
    callApiInput,
    executeActionInput,
    executeToolByNameInput,
    executeToolInput,
    executeWorkflowInput,
    getWorkflowRunInput,
    proxyInput,
} from './validators';

const middleware = bytechefMiddleware();

/**
 * Calls an endpoint of an API collection bound to the app. Resolves with the status, headers and body the endpoint's
 * workflow chose, a 4xx or 5xx included.
 */
export const callApi = createServerFn({method: 'POST'})
    .middleware([middleware])
    .validator(callApiInput)
    .handler(callApiHandler);
export const executeAction = createServerFn({method: 'POST'})
    .middleware([middleware])
    .validator(executeActionInput)
    .handler(executeActionHandler);
export const executeTool = createServerFn({method: 'POST'})
    .middleware([middleware])
    .validator(executeToolInput)
    .handler(executeToolHandler);
export const executeToolByName = createServerFn({method: 'POST'})
    .middleware([middleware])
    .validator(executeToolByNameInput)
    .handler(executeToolByNameHandler);
/**
 * Starts a run and returns it after the server's `waitSeconds` hold: `RUNNING` or `APPROVAL_REQUIRED` for a long
 * run. Poll with `getWorkflowRun`. For a server-side wait, write your own server function on `bytechefMiddleware()`
 * and pass a signal.
 */
export const executeWorkflow = createServerFn({method: 'POST'})
    .middleware([middleware])
    .validator(executeWorkflowInput)
    .handler(executeWorkflowHandler);
export const getWorkflowRun = createServerFn({method: 'GET'})
    .middleware([middleware])
    .validator(getWorkflowRunInput)
    .handler(getWorkflowRunHandler);
export const listApiCollections = createServerFn({method: 'GET'})
    .middleware([middleware])
    .handler(listApiCollectionsHandler);
export const listConnections = createServerFn({method: 'GET'}).middleware([middleware]).handler(listConnectionsHandler);
export const listTools = createServerFn({method: 'GET'}).middleware([middleware]).handler(listToolsHandler);
export const listWorkflows = createServerFn({method: 'GET'}).middleware([middleware]).handler(listWorkflowsHandler);
export const proxy = createServerFn({method: 'POST'})
    .middleware([middleware])
    .validator(proxyInput)
    .handler(proxyHandler);
