import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, describe, expect, it, vi} from 'vitest';

import ComponentConfigDialog, {ComponentConfigDialogTargetI} from './ComponentConfigDialog';

beforeAll(() => {
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

// Every query mock hands back a FROZEN, hoisted result rather than a fresh object literal per call. The
// dialog's form hook memoises its property list off the query result and resets the form whenever that memo
// changes, so a new object per render is an infinite render loop, not just a wasted render.
const {CLUSTER_ELEMENT_DEFINITION_RESULT, CONNECTIONS_RESULT, EMPTY_LIST_RESULT, EMPTY_RESULT} = vi.hoisted(() => ({
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
}));

// Stands in for the workflow editor's property renderer, printing the names it was handed so the dialog's own
// decision about WHICH properties to render stays observable without the editor in the test.
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
    ClusterElementProvider: ({children}: {children: ReactNode}) => <>{children}</>,
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
    useGetComponentDefinitionQuery: () => EMPTY_RESULT,
}));

vi.mock('@/shared/queries/platform/triggerDefinitions.queries', () => ({
    useGetTriggerDefinitionQuery: () => EMPTY_RESULT,
}));

vi.mock('@/shared/queries/platform/useFormDisplayConditions', () => ({
    default: () => undefined,
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
    it('renders every property of the target by default', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<ComponentConfigDialog onClose={vi.fn()} onSubmit={vi.fn()} open target={TARGET} workspaceId={7} />);

        await user.click(screen.getByRole('tab', {name: 'Properties'}));

        expect(screen.getByText('property:model')).toBeInTheDocument();
        expect(screen.getByText('property:temperature')).toBeInTheDocument();
    });

    // A caller that owns a field outside the tabs — the model dialog picks the model in its own combobox —
    // would otherwise show it twice, the second copy carrying the schema default rather than the real value.
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

    // Hiding the field is not enough on its own: an excluded name still reaching the submitted map would keep
    // writing back whatever was seeded or previously saved under it, which is the value the caller owns.
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
