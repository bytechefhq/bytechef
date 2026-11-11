import {describe, expect, it} from 'vitest';

import {collectTasks, getEditorGuideItems, isDeploymentLive} from '../getEditorGuideItems';

const baseInput = {
    executionsViewed: false,
    guideProjectPublished: false,
    httpClientTested: false,
    tasks: [] as {name: string; parameters?: Record<string, unknown>; type: string}[],
};

const emptyStatus = {
    completed: false,
    deployed: false,
    firstRun: null,
    httpClientAdded: false,
    lastFailedRun: null,
    live: false,
    loggerAdded: false,
    projectCreated: false,
    published: false,
    workflowCreated: false,
};

const liveDeployment = {
    enabled: true,
    id: 1,
    name: 'd',
    projectDeploymentWorkflows: [{enabled: true, workflowId: 'w1'}],
};

function doneById(input: Parameters<typeof getEditorGuideItems>[0]) {
    return Object.fromEntries(getEditorGuideItems(input).map((item) => [item.id, item.done]));
}

describe('getEditorGuideItems', () => {
    it('returns eight items in order', () => {
        expect(getEditorGuideItems(baseInput).map((item) => item.id)).toEqual([
            'fetch',
            'test',
            'log',
            'publish',
            'deploy',
            'enable',
            'run',
            'executions',
        ]);
    });

    it('marks fetch done once the GET step has a uri, or once published', () => {
        const getTask = (parameters?: Record<string, unknown>) => ({
            name: 'httpClient_1',
            parameters,
            type: 'httpClient/v2/get',
        });

        expect(doneById(baseInput).fetch).toBe(false);
        expect(doneById({...baseInput, tasks: [getTask()]}).fetch).toBe(false);
        expect(doneById({...baseInput, tasks: [getTask({uri: ' '})]}).fetch).toBe(false);
        expect(doneById({...baseInput, tasks: [getTask({uri: 'https://dummyjson.com/quotes/random'})]}).fetch).toBe(
            true
        );
        expect(
            doneById({
                ...baseInput,
                tasks: [{name: 'httpClient_1', parameters: {uri: 'https://x'}, type: 'httpClient/v2/post'}],
            }).fetch
        ).toBe(false);
        expect(doneById({...baseInput, status: {...emptyStatus, httpClientAdded: true}}).fetch).toBe(false);
        expect(
            doneById({...baseInput, guideProjectPublished: true, status: {...emptyStatus, published: true}}).fetch
        ).toBe(true);
        expect(getEditorGuideItems(baseInput)[0].copyValue).toBe('https://dummyjson.com/quotes/random');
    });

    it('marks log done once the Logger has text, or once published, and carries the data pill', () => {
        const loggerTask = (parameters?: Record<string, unknown>) => ({
            name: 'logger_1',
            parameters,
            type: 'logger/v1/info',
        });

        expect(doneById(baseInput).log).toBe(false);
        expect(doneById({...baseInput, tasks: [loggerTask()]}).log).toBe(false);
        expect(doneById({...baseInput, tasks: [loggerTask({text: '${httpClient_1.quote}'})]}).log).toBe(true);
        expect(doneById({...baseInput, status: {...emptyStatus, loggerAdded: true}}).log).toBe(false);
        expect(
            doneById({...baseInput, guideProjectPublished: true, status: {...emptyStatus, published: true}}).log
        ).toBe(true);
        expect(getEditorGuideItems(baseInput)[2].copyValue).toBe('${httpClient_1.quote}');
    });

    it('points the log step at the quote data pill and offers the expression as a fallback', () => {
        const logItem = getEditorGuideItems(baseInput)[2];

        expect(logItem.hint).toContain('pick quote under HTTP Client in the Data Pill Panel');
        expect(logItem.copyLabel).toBe('Or paste');
    });

    it('marks test done when the HTTP Client action was tested or published', () => {
        expect(doneById(baseInput).test).toBe(false);
        expect(doneById({...baseInput, httpClientTested: true}).test).toBe(true);
        expect(
            doneById({...baseInput, guideProjectPublished: true, status: {...emptyStatus, published: true}}).test
        ).toBe(true);
    });

    it('marks publish done when published', () => {
        expect(doneById(baseInput).publish).toBe(false);
        expect(
            doneById({...baseInput, guideProjectPublished: true, status: {...emptyStatus, published: true}}).publish
        ).toBe(true);
    });

    it('keeps the build steps and publish open until the guide project itself is published', () => {
        const done = doneById({...baseInput, status: {...emptyStatus, published: true}});

        expect([done.fetch, done.test, done.log, done.publish]).toEqual([false, false, false, false]);
    });

    it('marks deploy done when the tenant has a deployment and includes the environment label', () => {
        expect(doneById(baseInput).deploy).toBe(false);
        expect(
            doneById({...baseInput, deployment: liveDeployment, status: {...emptyStatus, deployed: true}}).deploy
        ).toBe(true);
        expect(getEditorGuideItems(baseInput)[4].label).toContain('Development');
    });

    it('keeps deploy open when the selected environment has no deployment of the guide project', () => {
        expect(doneById({...baseInput, status: {...emptyStatus, deployed: true, live: true}}).deploy).toBe(false);
    });

    it('marks enable done when the tenant and the selected environment have a live deployment', () => {
        expect(
            doneById({...baseInput, deployment: liveDeployment, status: {...emptyStatus, deployed: true}}).enable
        ).toBe(false);
        expect(doneById({...baseInput, deployment: liveDeployment, status: {...emptyStatus, live: true}}).enable).toBe(
            true
        );
    });

    it('keeps enable open when the deployment in the selected environment is not live', () => {
        const liveStatus = {...emptyStatus, deployed: true, live: true};

        expect(doneById({...baseInput, status: liveStatus}).enable).toBe(false);
        expect(
            doneById({...baseInput, deployment: {...liveDeployment, enabled: false}, status: liveStatus}).enable
        ).toBe(false);
    });

    it('marks run done when a first run exists', () => {
        expect(doneById(baseInput).run).toBe(false);
        expect(doneById({...baseInput, status: {...emptyStatus, completed: true}}).run).toBe(true);
    });

    it('marks every item done once a first run exists', () => {
        const status = {
            ...emptyStatus,
            firstRun: {
                date: 'd',
                errorMessage: null,
                jobId: '1',
                projectDeploymentEnabled: true,
                projectDeploymentId: '1',
                status: 'COMPLETED',
            },
        };

        const doneAfterRun = doneById({...baseInput, status});

        expect(Object.entries(doneAfterRun).every(([itemId, done]) => itemId === 'executions' || done)).toBe(true);
        expect(doneAfterRun.executions).toBe(false);
        expect(doneById({...baseInput, executionsViewed: true, status}).executions).toBe(true);
    });
});

describe('isDeploymentLive', () => {
    it('requires deployment and all workflows enabled', () => {
        expect(isDeploymentLive(liveDeployment)).toBe(true);
        expect(
            isDeploymentLive({
                ...liveDeployment,
                projectDeploymentWorkflows: [{enabled: false, workflowId: 'w1'}],
            })
        ).toBe(false);
        expect(isDeploymentLive({...liveDeployment, enabled: false})).toBe(false);
        expect(isDeploymentLive({...liveDeployment, projectDeploymentWorkflows: []})).toBe(false);
        expect(isDeploymentLive(undefined)).toBe(false);
    });
});

describe('collectTasks', () => {
    it('flattens nested tasks', () => {
        const tasks = [
            {
                name: 'loop_1',
                parameters: {iteratee: [{name: 'logger_1', type: 'logger/v1/info'}]},
                type: 'loop/v1',
            },
            {name: 'httpClient_1', type: 'httpClient/v1/get'},
        ];

        expect(collectTasks(tasks).map((task) => task.type)).toEqual([
            'loop/v1',
            'logger/v1/info',
            'httpClient/v1/get',
        ]);
    });
});
