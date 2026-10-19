import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import PlaceholderNode from './PlaceholderNode';

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    Handle: () => null,
}));

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

vi.mock('../components/WorkflowNodesPopoverMenuComponentList', () => ({
    default: () => null,
}));

vi.mock('../components/WorkflowNodesPopoverMenuOperationList', () => ({
    default: ({componentDefinition}: {componentDefinition: ComponentDefinition}) => (
        <div data-testid="operation-list">{componentDefinition.name}</div>
    ),
}));

const MODEL_PLACEHOLDER_ID = 'aiAgent_1-model-placeholder-0';

const MODEL_PLACEHOLDER_DATA = {
    clusterElementLabel: 'Model',
    clusterElementType: 'model',
    label: '+',
} as unknown as NodeDataType;

const OPEN_AI_COMPONENT_DEFINITION = {name: 'openAi', version: 1} as ComponentDefinition;

function renderPlaceholder() {
    return render(
        <QueryClientProvider client={new QueryClient()}>
            <PlaceholderNode data={MODEL_PLACEHOLDER_DATA} id={MODEL_PLACEHOLDER_ID} />
        </QueryClientProvider>
    );
}

describe('PlaceholderNode', () => {
    beforeEach(() => {
        useWorkflowEditorStore.setState({pendingClusterElementDrop: undefined});
    });

    it('opens its menu on the dropped component when a drop left the element choice to the user', async () => {
        useWorkflowEditorStore.setState({
            pendingClusterElementDrop: {
                componentDefinition: OPEN_AI_COMPONENT_DEFINITION,
                placeholderId: MODEL_PLACEHOLDER_ID,
            },
        });

        renderPlaceholder();

        expect(await screen.findByTestId('operation-list')).toHaveTextContent('openAi');
    });

    it('keeps its menu closed for a drop left pending on another placeholder', () => {
        useWorkflowEditorStore.setState({
            pendingClusterElementDrop: {
                componentDefinition: OPEN_AI_COMPONENT_DEFINITION,
                placeholderId: 'aiAgent_1-tools-placeholder-0',
            },
        });

        renderPlaceholder();

        expect(screen.queryByTestId('operation-list')).not.toBeInTheDocument();
    });

    it('forgets the pending drop once its menu is closed', async () => {
        useWorkflowEditorStore.setState({
            pendingClusterElementDrop: {
                componentDefinition: OPEN_AI_COMPONENT_DEFINITION,
                placeholderId: MODEL_PLACEHOLDER_ID,
            },
        });

        renderPlaceholder();

        await screen.findByTestId('operation-list');

        await userEvent.keyboard('{Escape}');

        await waitFor(() => expect(useWorkflowEditorStore.getState().pendingClusterElementDrop).toBeUndefined());
        expect(screen.queryByTestId('operation-list')).not.toBeInTheDocument();
    });
});
