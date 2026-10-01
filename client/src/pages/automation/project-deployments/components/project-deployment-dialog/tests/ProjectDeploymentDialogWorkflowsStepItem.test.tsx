import {TooltipProvider} from '@/components/ui/tooltip';
import {ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {render} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDialogWorkflowsStepItem from '../ProjectDeploymentDialogWorkflowsStepItem';

vi.mock('@/pages/automation/agents/hooks/useAgents', () => ({
    default: () => ({agents: [{id: '7', projectId: '1', projectWorkflowUuid: 'agent-workflow-uuid', title: 'uuuu'}]}),
}));

vi.mock('@/shared/components/ConnectionConfigurationList', () => ({default: () => null}));

vi.mock('@/shared/components/InputConfigurationList', () => ({default: () => null}));

const Harness = ({workflow}: {workflow: Workflow}) => {
    const {control, formState, setValue} = useForm<ProjectDeployment>();

    return (
        <TooltipProvider>
            <ProjectDeploymentDialogWorkflowsStepItem
                control={control}
                formState={formState}
                setValue={setValue}
                showWorkflowToggle
                workflow={workflow}
                workflowIndex={0}
                workflows={[workflow]}
            />
        </TooltipProvider>
    );
};

describe('ProjectDeploymentDialogWorkflowsStepItem', () => {
    it('marks an agent generated workflow with the agent icon', () => {
        const {container} = render(
            <Harness workflow={{id: 'workflow-1', label: 'uuuu', workflowUuid: 'agent-workflow-uuid'} as Workflow} />
        );

        expect(container.querySelector('svg.lucide-bot')).not.toBeNull();
        expect(container.querySelector('svg.lucide-workflow')).toBeNull();
    });

    it('keeps the workflow icon for an ordinary workflow', () => {
        const {container} = render(
            <Harness workflow={{id: 'workflow-2', label: 'Test Workflow1', workflowUuid: 'plain-uuid'} as Workflow} />
        );

        expect(container.querySelector('svg.lucide-workflow')).not.toBeNull();
        expect(container.querySelector('svg.lucide-bot')).toBeNull();
    });
});
