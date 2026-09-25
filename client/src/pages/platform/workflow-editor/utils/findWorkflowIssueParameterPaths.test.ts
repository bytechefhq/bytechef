import {describe, expect, it} from 'vitest';

import findWorkflowIssueParameterPaths, {getIssueParameterNames} from './findWorkflowIssueParameterPaths';

describe('findWorkflowIssueParameterPaths', () => {
    const parameters = {
        maxTokens: 100000,
        response: {responseSchema: '{}'},
        topK: '${firecrawl_5.data.json.result}',
        userPrompt: 'Summarize ${firecrawl_6.data.html} and ${firecrawl_5.data.title}',
    };

    it('finds the parameter holding the data pill a validator issue names', () => {
        expect(findWorkflowIssueParameterPaths({propertyPath: 'firecrawl_5.data.json.result'}, parameters)).toEqual([
            'topK',
        ]);
    });

    it('treats a property path that is a parameter as the parameter itself', () => {
        expect(findWorkflowIssueParameterPaths({propertyPath: 'response.responseSchema'}, parameters)).toEqual([
            'response.responseSchema',
        ]);
    });

    it('finds every parameter that references a named node, in nested objects and arrays too', () => {
        expect(
            findWorkflowIssueParameterPaths(
                {referencedNodeName: 'firecrawl_5'},
                {...parameters, attachments: [{content: '${firecrawl_5.data.file}'}]}
            )
        ).toEqual(['topK', 'userPrompt', 'attachments[0].content']);
    });

    it('does not match a node whose name only starts with the referenced one', () => {
        expect(
            findWorkflowIssueParameterPaths({referencedNodeName: 'firecrawl_5'}, {text: '${firecrawl_50.a}'})
        ).toEqual([]);
    });

    it('does not look inside the tasks a dispatcher holds', () => {
        expect(
            findWorkflowIssueParameterPaths(
                {referencedNodeName: 'firecrawl_5'},
                {
                    branches: [[{name: 'logger_1', parameters: {text: '${firecrawl_5}'}, type: 'logger/v1/info'}]],
                    caseTrue: [{name: 'logger_2', parameters: {text: '${firecrawl_5}'}, type: 'logger/v1/info'}],
                }
            )
        ).toEqual([]);
    });

    it('finds nothing for a missing property, an unknown path or no parameters', () => {
        expect(findWorkflowIssueParameterPaths({propertyPath: 'model'}, parameters)).toEqual([]);
        expect(findWorkflowIssueParameterPaths({}, parameters)).toEqual([]);
        expect(findWorkflowIssueParameterPaths({propertyPath: 'topK'}, undefined)).toEqual([]);
    });

    it("collects the top-level parameters a node's issues concern, including a missing one named by path", () => {
        expect(
            getIssueParameterNames(
                [
                    {propertyPath: 'firecrawl_5.data.json.result'},
                    {referencedNodeName: 'firecrawl_6'},
                    {propertyPath: 'stopSequences'},
                ],
                parameters
            )
        ).toEqual(new Set(['firecrawl_5', 'topK', 'userPrompt', 'stopSequences']));
    });
});
