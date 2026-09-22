import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DescriptionTab from './DescriptionTab';

const {generateMock, saveWorkflowDefinitionMock} = vi.hoisted(() => ({
    generateMock: vi.fn(),
    saveWorkflowDefinitionMock: vi.fn(),
}));

vi.mock('@/shared/components/copilot/useGenerateWorkflowDescription', () => ({
    useGenerateWorkflowDescription: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) => selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('../../utils/saveWorkflowDefinition', () => ({
    default: saveWorkflowDefinitionMock,
}));

const nodeDefinition = {name: 'httpClient', title: 'HTTP Client', version: 1} as ComponentDefinition;

const createNode = (workflowNodeName: string) =>
    ({
        componentName: 'httpClient',
        name: workflowNodeName,
        type: 'httpClient/v1/get',
        workflowNodeName,
    }) as NodeDataType;

describe('DescriptionTab', () => {
    beforeEach(() => {
        generateMock.mockReset();
        saveWorkflowDefinitionMock.mockReset();

        useWorkflowDataStore.setState({
            nodes: [],
            workflow: {
                id: 'workflow-1',
                tasks: [
                    {description: 'Notes of A', label: 'Node A', name: 'nodeA', type: 'httpClient/v1/get'},
                    {description: 'Notes of B', label: 'Node B', name: 'nodeB', type: 'httpClient/v1/get'},
                ],
            },
        } as never);

        useWorkflowNodeDetailsPanelStore.setState({currentNode: createNode('nodeA')});
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    const renderDescriptionTab = () =>
        render(
            <DescriptionTab nodeDefinition={nodeDefinition} updateWorkflowMutation={{} as UpdateWorkflowMutationType} />
        );

    it('applies and saves the generated notes on the node that started the generation', async () => {
        vi.useFakeTimers();

        generateMock.mockResolvedValue({value: 'Generated notes'});

        renderDescriptionTab();

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await act(async () => {
            await vi.runAllTimersAsync();
        });

        expect(generateMock).toHaveBeenCalledWith({
            environmentId: expect.any(Number),
            workflowId: 'workflow-1',
            workflowNodeName: 'nodeA',
        });
        expect(screen.getByPlaceholderText('Write some notes for yourself...')).toHaveValue('Generated notes');
        expect(saveWorkflowDefinitionMock).toHaveBeenCalledTimes(1);
        expect(saveWorkflowDefinitionMock.mock.calls[0][0].nodeData).toMatchObject({
            description: 'Generated notes',
            name: 'nodeA',
        });
    });

    it('discards the generated notes when another node of the same component and type is selected first', async () => {
        vi.useFakeTimers();

        let resolveGeneration: (result: {value: string}) => void = () => {};

        generateMock.mockReturnValue(
            new Promise((resolve) => {
                resolveGeneration = resolve;
            })
        );

        renderDescriptionTab();

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        act(() => {
            useWorkflowNodeDetailsPanelStore.setState({currentNode: createNode('nodeB')});
        });

        await act(async () => {
            resolveGeneration({value: 'Generated notes of A'});

            await vi.runAllTimersAsync();
        });

        expect(screen.getByPlaceholderText('Write some notes for yourself...')).not.toHaveValue('Generated notes of A');
        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });
});
