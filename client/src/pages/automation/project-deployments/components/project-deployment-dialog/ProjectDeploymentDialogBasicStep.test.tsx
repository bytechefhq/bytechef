import {Form} from '@/components/ui/form';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDialogBasicStep from './ProjectDeploymentDialogBasicStep';

const projects = [
    {id: 1, name: 'Learn ByteChef by doing'},
    {id: 2, name: 'AI Agent'},
];

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: projects}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: undefined}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: () => null,
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectsComboBox',
    () => ({
        default: ({onChange}: {onChange: (item: {name: string; value: number}) => void}) => (
            <>
                {projects.map((project) => (
                    <button
                        key={project.id}
                        onClick={() => onChange({name: project.name, value: project.id})}
                        type="button"
                    >
                        {`Choose ${project.name}`}
                    </button>
                ))}
            </>
        ),
    })
);

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepProjectVersionsSelect',
    () => ({
        default: ({onChange}: {onChange: (value: number) => void}) => (
            <button onClick={() => onChange(2)} type="button">
                Choose V2
            </button>
        ),
    })
);

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStepTagsSelect',
    () => ({
        default: () => null,
    })
);

const BasicStepHarness = () => {
    const form = useForm<ProjectDeployment>({defaultValues: {name: ''}});

    return (
        <Form {...form}>
            <ProjectDeploymentDialogBasicStep
                basicStepTab="new-deployment"
                changeProjectVersion={false}
                control={form.control}
                getValues={form.getValues}
                handleTabChange={vi.fn()}
                projectDeployment={undefined}
                setValue={form.setValue}
            />

            <button onClick={() => form.trigger()} type="button">
                Next
            </button>
        </Form>
    );
};

describe('ProjectDeploymentDialogBasicStep', () => {
    it('should clear the version error once a version is selected', async () => {
        render(<BasicStepHarness />);

        await userEvent.click(screen.getByRole('button', {name: 'Choose Learn ByteChef by doing'}));
        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        expect(screen.getByText('Version')).toHaveClass('text-destructive');

        await userEvent.click(screen.getByRole('button', {name: 'Choose AI Agent'}));
        await userEvent.click(screen.getByRole('button', {name: 'Choose V2'}));

        expect(screen.getByText('Version')).not.toHaveClass('text-destructive');
    });

    it('should replace an automatically filled name when another project is selected', async () => {
        render(<BasicStepHarness />);

        await userEvent.click(screen.getByRole('button', {name: 'Choose Learn ByteChef by doing'}));

        expect(screen.getByPlaceholderText('My CRM Project')).toHaveValue('Learn ByteChef by doing');

        await userEvent.click(screen.getByRole('button', {name: 'Choose AI Agent'}));

        expect(screen.getByPlaceholderText('My CRM Project')).toHaveValue('AI Agent');
    });

    it('should keep a name the user typed when another project is selected', async () => {
        render(<BasicStepHarness />);

        await userEvent.type(screen.getByPlaceholderText('My CRM Project'), 'My deployment');
        await userEvent.click(screen.getByRole('button', {name: 'Choose Learn ByteChef by doing'}));
        await userEvent.click(screen.getByRole('button', {name: 'Choose AI Agent'}));

        expect(screen.getByPlaceholderText('My CRM Project')).toHaveValue('My deployment');
    });
});
