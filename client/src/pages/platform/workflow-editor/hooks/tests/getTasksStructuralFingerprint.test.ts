import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getTasksStructuralFingerprint} from '../useLayout';

function makeTask(overrides: Partial<WorkflowTask> & {name: string; type: string}): WorkflowTask {
    return overrides as WorkflowTask;
}

describe('getTasksStructuralFingerprint', () => {
    it('should produce the same fingerprint for tasks differing only in parameter values', () => {
        const tasksA = [makeTask({name: 'http_1', parameters: {url: 'http://a.com'}, type: 'httpClient/v1/get'})];
        const tasksB = [makeTask({name: 'http_1', parameters: {url: 'http://b.com'}, type: 'httpClient/v1/get'})];

        expect(getTasksStructuralFingerprint(tasksA)).toBe(getTasksStructuralFingerprint(tasksB));
    });

    it('should produce different fingerprints when task names differ', () => {
        const tasksA = [makeTask({name: 'http_1', type: 'httpClient/v1/get'})];
        const tasksB = [makeTask({name: 'http_2', type: 'httpClient/v1/get'})];

        expect(getTasksStructuralFingerprint(tasksA)).not.toBe(getTasksStructuralFingerprint(tasksB));
    });

    it('should produce different fingerprints when clusterRoot differs', () => {
        const tasksA = [makeTask({clusterRoot: true, name: 'ds_1', type: 'dataStream/v1/stream'})];
        const tasksB = [makeTask({clusterRoot: false, name: 'ds_1', type: 'dataStream/v1/stream'})];

        expect(getTasksStructuralFingerprint(tasksA)).not.toBe(getTasksStructuralFingerprint(tasksB));
    });

    it('should produce different fingerprints when clusterElements presence differs', () => {
        const withElements = [
            makeTask({
                clusterElements: {source: [{name: 'csv_1', type: 'csvFile/v1/read'}]} as Record<string, unknown>,
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];
        const withoutElements = [
            makeTask({
                clusterElements: {source: []} as Record<string, unknown>,
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];

        expect(getTasksStructuralFingerprint(withElements)).not.toBe(getTasksStructuralFingerprint(withoutElements));
    });

    it('should treat empty clusterElements the same as no clusterElements', () => {
        const withEmpty = [
            makeTask({
                clusterElements: {} as Record<string, unknown>,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];
        const withNone = [
            makeTask({
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];

        expect(getTasksStructuralFingerprint(withEmpty)).toBe(getTasksStructuralFingerprint(withNone));
    });

    it('should treat clusterElements with only null/empty-array values as not filled', () => {
        const withNullValues = [
            makeTask({
                clusterElements: {processor: null, sink: [], source: null} as unknown as Record<string, unknown>,
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];
        const withoutElements = [
            makeTask({
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];

        expect(getTasksStructuralFingerprint(withNullValues)).toBe(getTasksStructuralFingerprint(withoutElements));
    });

    it('should produce different fingerprints when a nested cluster element is replaced', () => {
        const makeAiAgentTask = (vectorStoreType: string, vectorStoreName: string) =>
            makeTask({
                clusterElements: {
                    model: {name: 'openAi_1', type: 'openAi/v1/model'},
                    rag: {
                        clusterElements: {
                            vectorStore: {name: vectorStoreName, type: vectorStoreType},
                        },
                        name: 'questionAnswerRag_1',
                        type: 'questionAnswerRag/v1/rag',
                    },
                } as Record<string, unknown>,
                clusterRoot: true,
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            });

        expect(getTasksStructuralFingerprint([makeAiAgentTask('pgVector/v1/vectorStore', 'pgVector_1')])).not.toBe(
            getTasksStructuralFingerprint([makeAiAgentTask('knowledgeBase/v1/vectorStore', 'knowledgeBase_1')])
        );
    });

    it('should produce different fingerprints when a cluster element is added to a filled slot', () => {
        const oneTool = [
            makeTask({
                clusterElements: {tools: [{name: 'dateHelper_1', type: 'dateHelper/v1/getCurrentDate'}]} as Record<
                    string,
                    unknown
                >,
                clusterRoot: true,
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            }),
        ];
        const twoTools = [
            makeTask({
                clusterElements: {
                    tools: [
                        {name: 'dateHelper_1', type: 'dateHelper/v1/getCurrentDate'},
                        {name: 'googleCalendar_1', type: 'googleCalendar/v1/createEvent'},
                    ],
                } as Record<string, unknown>,
                clusterRoot: true,
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            }),
        ];

        expect(getTasksStructuralFingerprint(oneTool)).not.toBe(getTasksStructuralFingerprint(twoTools));
    });

    it('should produce the same fingerprint for cluster elements differing only in parameter values', () => {
        const makeAiAgentTask = (systemPrompt: string) =>
            makeTask({
                clusterElements: {
                    model: {name: 'openAi_1', parameters: {systemPrompt}, type: 'openAi/v1/model'},
                } as Record<string, unknown>,
                clusterRoot: true,
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            });

        expect(getTasksStructuralFingerprint([makeAiAgentTask('a')])).toBe(
            getTasksStructuralFingerprint([makeAiAgentTask('b')])
        );
    });

    it('should produce the same fingerprint for the workflow definition and workflow task cluster element shapes', () => {
        const definitionShape = [
            makeTask({
                clusterElements: {
                    rag: {
                        clusterElements: {
                            vectorStore: {name: 'knowledgeBase_1', type: 'knowledgeBase/v1/vectorStore'},
                        },
                        name: 'questionAnswerRag_1',
                        type: 'questionAnswerRag/v1/rag',
                    },
                } as Record<string, unknown>,
                clusterRoot: true,
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            }),
        ];
        const taskShape = [
            makeTask({
                clusterElements: {
                    rag: {
                        extensions: {
                            clusterElements: {
                                vectorStore: {name: 'knowledgeBase_1', type: 'knowledgeBase/v1/vectorStore'},
                            },
                        },
                        type: 'questionAnswerRag/v1/rag',
                        workflowNodeName: 'questionAnswerRag_1',
                    },
                } as Record<string, unknown>,
                clusterRoot: true,
                name: 'aiAgent_1',
                type: 'aiAgent/v1/chat',
            }),
        ];

        expect(getTasksStructuralFingerprint(definitionShape)).toBe(getTasksStructuralFingerprint(taskShape));
    });
});
