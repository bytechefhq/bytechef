import {describe, expect, it} from 'vitest';

import {parseWorkflowNodeLookupUrl} from '../workflowNodeLookupUrl';

describe('parseWorkflowNodeLookupUrl', () => {
    it('parses a node options lookup', () => {
        expect(
            parseWorkflowNodeLookupUrl(
                'http://localhost/api/platform/internal/workflows/1052/workflow-nodes/dataTable_2/options/table?searchText='
            )
        ).toEqual({nodeName: 'dataTable_2', propertyName: 'table'});
    });

    it('parses a dynamic properties lookup', () => {
        expect(
            parseWorkflowNodeLookupUrl('/internal/workflows/1052/workflow-nodes/dataTable_2/dynamic-properties/values')
        ).toEqual({nodeName: 'dataTable_2', propertyName: 'values'});
    });

    it('attributes a cluster element options lookup to the cluster element node', () => {
        expect(
            parseWorkflowNodeLookupUrl(
                '/internal/workflows/1052/workflow-nodes/aiAgent_1/cluster-elements/model/openAi_1/options/model'
            )
        ).toEqual({nodeName: 'openAi_1', propertyName: 'model'});
    });

    it('attributes a cluster element dynamic properties lookup to the cluster element node', () => {
        expect(
            parseWorkflowNodeLookupUrl(
                '/internal/workflows/1052/workflow-nodes/aiAgent_1/cluster-elements/model/openAi_1/dynamic-properties/model'
            )
        ).toEqual({nodeName: 'openAi_1', propertyName: 'model'});
    });

    it('parses a node wide output schema failure, which names no property', () => {
        expect(parseWorkflowNodeLookupUrl('/internal/workflows/1052/workflow-nodes/dataTable_2/outputs')).toEqual({
            nodeName: 'dataTable_2',
        });
    });

    it('attributes a cluster element output schema failure to the cluster element node', () => {
        expect(
            parseWorkflowNodeLookupUrl(
                '/internal/workflows/1052/workflow-nodes/aiAgent_1/cluster-elements/model/openAi_1/outputs'
            )
        ).toEqual({nodeName: 'openAi_1'});
    });

    it('does not match the workflow wide outputs endpoint', () => {
        expect(parseWorkflowNodeLookupUrl('/internal/workflows/1052/outputs')).toBeUndefined();
    });

    it('ignores unrelated urls', () => {
        expect(parseWorkflowNodeLookupUrl('/internal/workflows/1052')).toBeUndefined();
        expect(parseWorkflowNodeLookupUrl('/graphql')).toBeUndefined();
        expect(parseWorkflowNodeLookupUrl('/internal/workflows/1052/workflow-nodes/dataTable_2')).toBeUndefined();
    });

    it('attributes a workflow wide outputs lookup to the node named in the query', () => {
        expect(
            parseWorkflowNodeLookupUrl(
                '/api/platform/internal/workflows/2035b93a/outputs?environmentId=0&lastWorkflowNodeName=dataTable_2'
            )
        ).toEqual({nodeName: 'dataTable_2'});
    });

    it('ignores a workflow wide outputs lookup that names no node', () => {
        expect(
            parseWorkflowNodeLookupUrl('/api/platform/internal/workflows/2035b93a/outputs?environmentId=0')
        ).toBeUndefined();
    });

    it('attributes a display conditions lookup to its node', () => {
        expect(
            parseWorkflowNodeLookupUrl(
                '/api/platform/internal/workflows/2035b93a/workflow-nodes/condition_1/display-conditions?environmentId=0'
            )
        ).toEqual({nodeName: 'condition_1'});
    });
});
