import {Form} from '@/components/ui/form';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDialogBasicStep from './ProjectDeploymentDialogBasicStep';

const projects = [
    {id: 1, name: 'Learn ByteChef by doing'},
    {id: 2, name: 'AI Agent'},
];

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({data: projects}),
}));

const hoisted = vi.hoisted(() => ({
    deployableEnvironmentIds: undefined as number[] | undefined,
    environments: undefined as {id: string; name: string}[] | undefined,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: hoisted.environments ? {environments: hoisted.environments} : undefined}),
}));

vi.mock('@/shared/hooks/useWorkspaceScopeEnvironmentIds', () => ({
    useWorkspaceScopeEnvironmentIds: () => hoisted.deployableEnvironmentIds,
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
        default: ({onChange, projectVersion}: {onChange: (value: number) => void; projectVersion?: number}) => (
            <>
                {projectVersion != null && <span>{`Selected V${projectVersion}`}</span>}

                <button onClick={() => onChange(2)} type="button">
                    Choose V2
                </button>
            </>
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

const EnvironmentEditableHarness = () => {
    const form = useForm<ProjectDeployment>({defaultValues: {name: ''}});

    return (
        <Form {...form}>
            <ProjectDeploymentDialogBasicStep
                basicStepTab="new-deployment"
                changeProjectVersion={false}
                control={form.control}
                environmentEditable
                getValues={form.getValues}
                handleTabChange={vi.fn()}
                projectDeployment={undefined}
                setValue={form.setValue}
            />
        </Form>
    );
};

const ChangeVersionHarness = ({
    onDeploymentSelect,
    projectDeployments,
}: {
    onDeploymentSelect: (deployment: ProjectDeployment) => void;
    projectDeployments: ProjectDeployment[];
}) => {
    const form = useForm<ProjectDeployment>({defaultValues: {name: '', projectId: 1}});

    return (
        <Form {...form}>
            <ProjectDeploymentDialogBasicStep
                basicStepTab="change-version"
                changeProjectVersion={false}
                control={form.control}
                getValues={form.getValues}
                handleTabChange={vi.fn()}
                onDeploymentSelect={onDeploymentSelect}
                projectDeployment={undefined}
                projectDeployments={projectDeployments}
                projectDeploymentsLoading={false}
                setValue={form.setValue}
                showTabs
            />
        </Form>
    );
};

const TabsHarness = ({projectDeployments}: {projectDeployments: ProjectDeployment[]}) => {
    const [basicStepTab, setBasicStepTab] = useState<'new-deployment' | 'change-version'>('change-version');

    const form = useForm<ProjectDeployment>({defaultValues: {name: '', projectId: 1}});

    const handleTabChange = (tab: 'new-deployment' | 'change-version') => {
        setBasicStepTab(tab);

        if (tab === 'new-deployment') {
            form.reset({name: '', projectId: 1, projectVersion: undefined});
        }
    };

    return (
        <Form {...form}>
            <ProjectDeploymentDialogBasicStep
                basicStepTab={basicStepTab}
                changeProjectVersion={false}
                control={form.control}
                getValues={form.getValues}
                handleTabChange={handleTabChange}
                onDeploymentSelect={vi.fn()}
                projectDeployment={undefined}
                projectDeployments={projectDeployments}
                projectDeploymentsLoading={false}
                setValue={form.setValue}
                showTabs
            />
        </Form>
    );
};

describe('ProjectDeploymentDialogBasicStep', () => {
    beforeEach(() => {
        hoisted.deployableEnvironmentIds = undefined;
        hoisted.environments = undefined;
    });

    describe('environment select', () => {
        beforeAll(() => {
            Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
            Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();
            Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
        });

        beforeEach(() => {
            hoisted.environments = [
                {id: '0', name: 'Development'},
                {id: '1', name: 'Staging'},
                {id: '2', name: 'Production'},
            ];
        });

        it('should offer only the environments in which the member may create deployments', async () => {
            hoisted.deployableEnvironmentIds = [0, 1];

            render(<EnvironmentEditableHarness />);

            await userEvent.click(screen.getByRole('combobox'));

            expect(screen.getByRole('option', {name: 'Development'})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: 'Staging'})).toBeInTheDocument();
            expect(screen.queryByRole('option', {name: 'Production'})).not.toBeInTheDocument();
        });

        it('should offer every environment when the member is unrestricted', async () => {
            render(<EnvironmentEditableHarness />);

            await userEvent.click(screen.getByRole('combobox'));

            expect(screen.getByRole('option', {name: 'Development'})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: 'Staging'})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: 'Production'})).toBeInTheDocument();
        });
    });

    it('should preselect the deployment of a project that has only one', () => {
        const onDeploymentSelect = vi.fn();
        const projectDeployment = {environmentId: 1, id: 7, name: 'Production', projectId: 1, projectVersion: 3};

        render(
            <ChangeVersionHarness
                onDeploymentSelect={onDeploymentSelect}
                projectDeployments={[projectDeployment] as ProjectDeployment[]}
            />
        );

        expect(onDeploymentSelect).toHaveBeenCalledWith(projectDeployment);
        expect(screen.getByText('Production - V3')).toBeInTheDocument();
    });

    it('should leave the deployment unselected when a project has several', () => {
        const onDeploymentSelect = vi.fn();

        render(
            <ChangeVersionHarness
                onDeploymentSelect={onDeploymentSelect}
                projectDeployments={
                    [
                        {environmentId: 1, id: 7, name: 'Production', projectId: 1, projectVersion: 3},
                        {environmentId: 1, id: 8, name: 'Staging', projectId: 1, projectVersion: 4},
                    ] as ProjectDeployment[]
                }
            />
        );

        expect(onDeploymentSelect).not.toHaveBeenCalled();
        expect(screen.getByText('Select deployment')).toBeInTheDocument();
    });

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

    it('should clear the version picked from an existing deployment when switching to the new tab', async () => {
        render(
            <TabsHarness
                projectDeployments={
                    [
                        {environmentId: 1, id: 7, name: 'Production', projectId: 1, projectVersion: 3},
                    ] as ProjectDeployment[]
                }
            />
        );

        expect(screen.getByText('Selected V3')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'New'}));

        expect(screen.queryByText('Selected V3')).not.toBeInTheDocument();
    });
});
