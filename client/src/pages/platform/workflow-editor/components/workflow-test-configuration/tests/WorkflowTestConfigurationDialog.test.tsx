import {TooltipProvider} from '@/components/ui/tooltip';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowTestConfigurationDialog from '../WorkflowTestConfigurationDialog';

const hoisted = vi.hoisted(() => ({
    saveMutate: vi.fn(),
}));

vi.mock('@/shared/mutations/platform/workflowTestConfigurations.mutations', () => ({
    useSaveWorkflowTestConfigurationMutation: () => ({mutate: hoisted.saveMutate, reset: vi.fn()}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        ConnectionKeys: {connections: ['connections']},
        updateWorkflowMutation: {mutate: vi.fn()},
        useCreateConnectionMutation: () => ({mutate: vi.fn(), reset: vi.fn()}),
        useGetComponentDefinitionsQuery: () => ({data: []}),
        useGetConnectionTagsQuery: () => ({data: []}),
        useGetConnectionsQuery: () => ({data: []}),
    }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({workflowTestConfiguration: undefined}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: Record<string, unknown>) => unknown) => selector({currentNode: undefined}),
}));

vi.mock('@/shared/components/ConnectionConfigurationList', () => ({
    default: () => <div data-testid="connection-configuration-list" />,
}));

vi.mock('@/shared/components/InputConfigurationList', () => ({
    default: () => <div data-testid="input-configuration-list" />,
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: () => <div data-testid="connection-dialog" />,
}));

// The dialog's info tooltips need the provider the app mounts at its root.
const TooltipHarness = ({children}: {children: ReactNode}) => <TooltipProvider>{children}</TooltipProvider>;

const onClose = vi.fn();

const workflow = {
    definition: JSON.stringify({inputs: [], tasks: [], triggers: []}),
    id: 'w1',
    inputs: [],
    label: 'My Workflow',
    tasks: [],
    triggers: [],
} as unknown as Workflow;

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('WorkflowTestConfigurationDialog', () => {
    describe('rendering', () => {
        it('should render the title and description', () => {
            render(
                <TooltipHarness>
                    <WorkflowTestConfigurationDialog onClose={onClose} workflow={workflow} />
                </TooltipHarness>
            );

            expect(screen.getByRole('heading', {name: 'Workflow Test Configuration'})).toBeInTheDocument();
            expect(
                screen.getByText('Set workflow input, trigger output values and test connections.')
            ).toBeInTheDocument();
        });

        it('should render the connections and inputs tabs', () => {
            render(
                <TooltipHarness>
                    <WorkflowTestConfigurationDialog onClose={onClose} workflow={workflow} />
                </TooltipHarness>
            );

            // Each tab's name includes the count it renders beside the label.
            expect(screen.getByRole('tab', {name: /Connections/})).toBeInTheDocument();
            expect(screen.getByRole('tab', {name: /Inputs/})).toBeInTheDocument();
        });

        it('should render the close control', () => {
            render(
                <TooltipHarness>
                    <WorkflowTestConfigurationDialog onClose={onClose} workflow={workflow} />
                </TooltipHarness>
            );

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(
                <TooltipHarness>
                    <WorkflowTestConfigurationDialog onClose={onClose} workflow={workflow} />
                </TooltipHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
