import {ClusterElementsType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import getClusterElementIconStatuses from './getClusterElementIconStatuses';

const CLUSTER_ELEMENTS = {
    model: {name: 'openAi_1', type: 'openAi/v1/model'},
    tools: [
        {name: 'httpClient_1', type: 'httpClient/v1/get'},
        {name: 'httpClient_2', type: 'httpClient/v1/get'},
        {name: 'slack_1', type: 'slack/v1/sendMessage'},
    ],
} as unknown as ClusterElementsType;

describe('getClusterElementIconStatuses', () => {
    it('gives an icon the worst status of the elements behind it', () => {
        expect(
            getClusterElementIconStatuses(CLUSTER_ELEMENTS, {
                httpClient_1: {status: 'COMPLETED'},
                httpClient_2: {status: 'FAILED'},
                slack_1: {status: 'COMPLETED'},
            })
        ).toEqual({'/icons/httpClient.svg': 'FAILED', '/icons/slack.svg': 'COMPLETED'});
    });

    it('prefers a running element over a completed one', () => {
        expect(
            getClusterElementIconStatuses(CLUSTER_ELEMENTS, {
                httpClient_1: {status: 'COMPLETED'},
                httpClient_2: {status: 'RUNNING'},
            })
        ).toEqual({'/icons/httpClient.svg': 'RUNNING'});
    });

    it('leaves out icons whose elements were not called', () => {
        expect(getClusterElementIconStatuses(CLUSTER_ELEMENTS, {})).toEqual({});
    });

    it('reads the element name the canvas node data carries', () => {
        expect(
            getClusterElementIconStatuses(
                {
                    tools: [{type: 'httpClient/v1/get', workflowNodeName: 'httpClient_1'}],
                } as unknown as ClusterElementsType,
                {httpClient_1: {status: 'COMPLETED'}}
            )
        ).toEqual({'/icons/httpClient.svg': 'COMPLETED'});
    });
});
