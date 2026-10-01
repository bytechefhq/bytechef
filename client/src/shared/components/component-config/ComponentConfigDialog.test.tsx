import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import ComponentConfigDialog, {ComponentConfigDialogTargetI} from './ComponentConfigDialog';

beforeAll(() => {
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {CLUSTER_ELEMENT_DEFINITION_RESULT, CONNECTIONS_RESULT, EMPTY_LIST_RESULT, EMPTY_RESULT, mutable} = vi.hoisted(
    () => ({
        CLUSTER_ELEMENT_DEFINITION_RESULT: {
            data: {
                clusterElementDefinition: {
                    properties: [
                        {defaultValue: 'gpt-3.5-turbo-0125', name: 'model', type: 'STRING'},
                        {name: 'temperature', numberDefaultValue: 0.7, type: 'NUMBER'},
                    ],
                },
            },
            isLoading: false,
        },
        CONNECTIONS_RESULT: {data: [], isLoading: false},
        EMPTY_LIST_RESULT: {data: []},
        EMPTY_RESULT: {data: undefined, isLoading: false},
        mutable: {
            clusterElementContextValue: {} as Record<string, unknown>,
            componentDefinitionResult: {data: undefined, isLoading: false} as Record<string, unknown>,
        },
    })
);

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: ({properties}: {properties: {name: string}[]}) => (
        <div>
            {properties.map((property) => (
                <span key={property.name}>property:{property.name}</span>
            ))}
        </div>
    ),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/ClusterElementContext', () => ({
    ClusterElementProvider: ({children, value}: {children: ReactNode; value: Record<string, unknown>}) => {
        mutable.clusterElementContextValue = value;

        return <>{children}</>;
    },
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: () => <div>connection-dialog</div>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useClusterElementDefinitionQuery: () => CLUSTER_ELEMENT_DEFINITION_RESULT,
}));

vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useCreateConnectionMutation: vi.fn(),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => EMPTY_LIST_RESULT,
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    ConnectionKeys: {connectionTags: () => ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => EMPTY_LIST_RESULT,
    useGetWorkspaceConnectionsQuery: () => CONNECTIONS_RESULT,
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => mutable.componentDefinitionResult,
}));

vi.mock('@/shared/queries/platform/triggerDefinitions.queries', () => ({
    useGetTriggerDefinitionQuery: () => EMPTY_RESULT,
}));

vi.mock('@/shared/queries/platform/useFormDisplayConditions', () => ({
    default: () => ({displayConditions: undefined, isEvaluating: false}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 123})),
}));

const TARGET: ComponentConfigDialogTargetI = {
    clusterElementName: 'model',
    componentName: 'openAi',
    componentVersion: 1,
};

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );
};

describe('ComponentConfigDialog', () => {
    beforeEach(() => {
        mutable.clusterElementContextValue = {};
        mutable.componentDefinitionResult = {data: undefined, isLoading: false};
    });

    it('carries the target definition connectionRequired into the cluster element context', async () => {
        mutable.componentDefinitionResult = {data: {connectionRequired: true}, isLoading: false};

        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<ComponentConfigDialog onClose={vi.fn()} onSubmit={vi.fn()} open target={TARGET} workspaceId={7} />);

        await user.click(screen.getByRole('tab', {name: 'Properties'}));

        expect(mutable.clusterElementContextValue.connectionRequired).toBe(true);
        expect(mutable.clusterElementContextValue.connectionId).toBeUndefined();
    });

    it('renders every property of the target by default', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<ComponentConfigDialog onClose={vi.fn()} onSubmit={vi.fn()} open target={TARGET} workspaceId={7} />);

        await user.click(screen.getByRole('tab', {name: 'Properties'}));

        expect(screen.getByText('property:model')).toBeInTheDocument();
        expect(screen.getByText('property:temperature')).toBeInTheDocument();
    });

    it('withholds an excluded property from the Properties tab', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(
            <ComponentConfigDialog
                excludedPropertyNames={['model']}
                onClose={vi.fn()}
                onSubmit={vi.fn()}
                open
                target={TARGET}
                workspaceId={7}
            />
        );

        await user.click(screen.getByRole('tab', {name: 'Properties'}));

        expect(screen.queryByText('property:model')).not.toBeInTheDocument();
        expect(screen.getByText('property:temperature')).toBeInTheDocument();
    });

    it('keeps an excluded property out of the submitted parameters, seeded or inherited', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});
        const onSubmit = vi.fn();

        wrap(
            <ComponentConfigDialog
                excludedPropertyNames={['model']}
                initialValues={{connectionId: null, parameters: {model: 'gpt-4o', temperature: 0.2}}}
                onClose={vi.fn()}
                onSubmit={onSubmit}
                open
                target={TARGET}
                workspaceId={7}
            />
        );

        await user.click(screen.getByRole('button', {name: 'Save'}));

        expect(onSubmit).toHaveBeenCalledWith({connectionId: null, parameters: {temperature: 0.2}});
    });
});
