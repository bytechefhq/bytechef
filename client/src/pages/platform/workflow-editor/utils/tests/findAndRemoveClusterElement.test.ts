import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import findAndRemoveClusterElement from '../findAndRemoveClusterElement';

function makeElement(name: string, type: string, clusterElements?: ClusterElementsType): ClusterElementItemType {
    return {clusterElements, name, type};
}

describe('findAndRemoveClusterElement', () => {
    it('removes an element from a first-level multiple-elements array', () => {
        const clusterElements: ClusterElementsType = {
            tools: [makeElement('tool_1', 'slack/v1/send'), makeElement('tool_2', 'gmail/v1/send')],
        };

        const result = findAndRemoveClusterElement({clickedElementName: 'tool_1', clusterElements});

        expect(result.elementFound).toBe(true);
        expect(result.elements.tools).toEqual([makeElement('tool_2', 'gmail/v1/send')]);
    });

    it('sets a first-level single element to null', () => {
        const clusterElements: ClusterElementsType = {
            model: makeElement('model_1', 'openAi/v1/model'),
            tools: [],
        };

        const result = findAndRemoveClusterElement({clickedElementName: 'model_1', clusterElements});

        expect(result.elementFound).toBe(true);
        expect(result.elements).toEqual({model: null, tools: []});
    });

    it('does not mutate the input cluster elements', () => {
        const tools = [makeElement('tool_1', 'slack/v1/send')];
        const model = makeElement('model_1', 'openAi/v1/model');
        const clusterElements: ClusterElementsType = {model, tools};

        findAndRemoveClusterElement({clickedElementName: 'tool_1', clusterElements});
        findAndRemoveClusterElement({clickedElementName: 'model_1', clusterElements});

        expect(clusterElements.tools).toBe(tools);
        expect(clusterElements.tools).toHaveLength(1);
        expect(clusterElements.model).toBe(model);
    });

    it('reports not found and returns an equal copy when nothing matches', () => {
        const clusterElements: ClusterElementsType = {
            memory: null,
            model: makeElement('model_1', 'openAi/v1/model'),
            tools: [makeElement('tool_1', 'slack/v1/send')],
        };

        const result = findAndRemoveClusterElement({clickedElementName: 'missing', clusterElements});

        expect(result.elementFound).toBe(false);
        expect(result.elements).toEqual(clusterElements);
        expect(result.elements).not.toBe(clusterElements);
    });

    it('only removes elements stored under the requested element type', () => {
        const clusterElements: ClusterElementsType = {
            model: makeElement('shared_1', 'openAi/v1/model'),
            tools: [makeElement('shared_1', 'slack/v1/send')],
        };

        const toolResult = findAndRemoveClusterElement({
            clickedElementName: 'shared_1',
            clickedElementType: 'tools',
            clusterElements,
        });

        expect(toolResult.elementFound).toBe(true);
        expect(toolResult.elements).toEqual({model: clusterElements.model, tools: []});

        const modelResult = findAndRemoveClusterElement({
            clickedElementName: 'shared_1',
            clickedElementType: 'model',
            clusterElements,
        });

        expect(modelResult.elementFound).toBe(true);
        expect(modelResult.elements).toEqual({model: null, tools: clusterElements.tools});

        const missingTypeResult = findAndRemoveClusterElement({
            clickedElementName: 'shared_1',
            clickedElementType: 'memory',
            clusterElements,
        });

        expect(missingTypeResult.elementFound).toBe(false);
        expect(missingTypeResult.elements).toEqual(clusterElements);
    });

    it('stops after the first element type in which the element is found', () => {
        const clusterElements: ClusterElementsType = {
            model: makeElement('shared_1', 'openAi/v1/model'),
            tools: [makeElement('shared_1', 'slack/v1/send')],
        };

        const result = findAndRemoveClusterElement({clickedElementName: 'shared_1', clusterElements});

        expect(result.elements).toEqual({model: null, tools: clusterElements.tools});
    });

    it('removes an element nested inside an element of a multiple-elements array', () => {
        const nestedAgent = makeElement('agent_1', 'aiAgent/v1/tool', {
            model: makeElement('model_2', 'openAi/v1/model'),
            tools: [makeElement('tool_2', 'slack/v1/send')],
        });
        const siblingTool = makeElement('tool_1', 'gmail/v1/send');

        const clusterElements: ClusterElementsType = {tools: [siblingTool, nestedAgent]};

        const result = findAndRemoveClusterElement({clickedElementName: 'tool_2', clusterElements});

        expect(result.elementFound).toBe(true);
        expect(result.elements.tools).toEqual([
            siblingTool,
            {...nestedAgent, clusterElements: {model: nestedAgent.clusterElements?.model, tools: []}},
        ]);
        expect((result.elements.tools as ClusterElementItemType[])[0]).toBe(siblingTool);
        expect(nestedAgent.clusterElements?.tools).toHaveLength(1);
    });

    it('removes a single element nested inside a single element', () => {
        const retriever = makeElement('retriever_1', 'vectorStore/v1/retriever', {
            embedding: makeElement('embedding_1', 'openAi/v1/embedding'),
        });

        const result = findAndRemoveClusterElement({
            clickedElementName: 'embedding_1',
            clusterElements: {retriever},
        });

        expect(result.elementFound).toBe(true);
        expect(result.elements.retriever).toEqual({...retriever, clusterElements: {embedding: null}});
        expect(retriever.clusterElements?.embedding).not.toBeNull();
    });

    it('applies the element type filter at the nested level', () => {
        const retriever = makeElement('retriever_1', 'vectorStore/v1/retriever', {
            embedding: makeElement('nested_1', 'openAi/v1/embedding'),
            tools: [makeElement('nested_1', 'slack/v1/send')],
        });

        const result = findAndRemoveClusterElement({
            clickedElementName: 'nested_1',
            clickedElementType: 'tools',
            clusterElements: {retriever},
        });

        expect(result.elementFound).toBe(true);
        expect(result.elements.retriever).toEqual({
            ...retriever,
            clusterElements: {embedding: retriever.clusterElements?.embedding, tools: []},
        });
    });

    it('removes an element nested several levels deep', () => {
        const clusterElements: ClusterElementsType = {
            tools: [
                makeElement('agent_1', 'aiAgent/v1/tool', {
                    retriever: makeElement('retriever_1', 'vectorStore/v1/retriever', {
                        embedding: makeElement('embedding_1', 'openAi/v1/embedding'),
                    }),
                }),
            ],
        };

        const result = findAndRemoveClusterElement({clickedElementName: 'embedding_1', clusterElements});

        const agent = (result.elements.tools as ClusterElementItemType[])[0];
        const retriever = agent.clusterElements?.retriever as ClusterElementItemType;

        expect(result.elementFound).toBe(true);
        expect(retriever.clusterElements).toEqual({embedding: null});
    });

    it('removes a matching single element as a whole instead of descending into it', () => {
        const model = makeElement('model_1', 'openAi/v1/model', {
            tools: [makeElement('model_1', 'slack/v1/send')],
        });

        const result = findAndRemoveClusterElement({clickedElementName: 'model_1', clusterElements: {model}});

        expect(result.elements).toEqual({model: null});
    });
});
