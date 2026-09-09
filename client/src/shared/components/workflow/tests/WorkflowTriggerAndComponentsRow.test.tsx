import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useGetComponentDefinitionQueryMock} = vi.hoisted(() => ({
    useGetComponentDefinitionQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: useGetComponentDefinitionQueryMock,
}));

const workflowComponentDefinitions = {
    googleMail: {icon: '<svg />', name: 'googleMail', title: 'Gmail'},
    openAi: {icon: '<svg />', name: 'openAi', title: 'OpenAI'},
    slack: {icon: '<svg />', name: 'slack', title: 'Slack'},
};

const renderRow = (workflow: Parameters<typeof WorkflowTriggerAndComponentsRow>[0]['workflow']) =>
    render(
        <TooltipProvider>
            <WorkflowTriggerAndComponentsRow
                filteredComponentNames={['googleMail', 'slack', 'openAi']}
                workflow={workflow}
                workflowComponentDefinitions={workflowComponentDefinitions}
                workflowTaskDispatcherDefinitions={{}}
            />
        </TooltipProvider>
    );

describe('WorkflowTriggerAndComponentsRow', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useGetComponentDefinitionQueryMock.mockReturnValue({data: undefined});
    });

    it('labels the trigger with the title from its definition', () => {
        useGetComponentDefinitionQueryMock.mockReturnValue({
            data: {triggers: [{description: 'Runs when a new email arrives', name: 'newEmail', title: 'New Email'}]},
        });

        renderRow({
            triggers: [{type: 'googleMail/v1/newEmail'}],
            workflowTriggerComponentNames: ['googleMail'],
        });

        expect(screen.getByText('New Email')).toBeInTheDocument();
    });

    it('falls back to the workflow trigger label when the definition has no matching trigger', () => {
        renderRow({
            triggers: [{label: 'Manual', type: 'manual/v1/manual'}],
            workflowTriggerComponentNames: ['manual'],
        });

        expect(screen.getByText('Manual')).toBeInTheDocument();
    });

    it('names the component when the trigger carries no label of its own', () => {
        renderRow({
            triggers: [{type: 'slack/v1/newMessage'}],
            workflowTriggerComponentNames: ['slack'],
        });

        expect(screen.getByText('Slack')).toBeInTheDocument();
    });

    it('leaves the trigger out of the component icons', () => {
        renderRow({
            triggers: [{label: 'Manual', type: 'manual/v1/manual'}],
            workflowTriggerComponentNames: ['manual'],
        });

        expect(screen.getAllByLabelText('Workflow component icon')).toHaveLength(2);
    });

    it('renders only the components for a workflow without a trigger', () => {
        renderRow({triggers: [], workflowTriggerComponentNames: []});

        expect(screen.getAllByLabelText('Workflow component icon')).toHaveLength(3);
    });
});
