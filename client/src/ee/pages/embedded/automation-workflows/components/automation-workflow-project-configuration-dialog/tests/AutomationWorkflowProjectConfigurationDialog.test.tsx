import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationDialog from '../AutomationWorkflowProjectConfigurationDialog';

const mocks = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/queries/platform/environments.queries', () => ({
    useGetEnvironmentsQuery: () => ({
        data: [
            {id: 1, name: 'Development'},
            {id: 2, name: 'Staging'},
        ],
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationsQuery: () => ({
        data: {automationWorkflowProjectConfigurations: []},
    }),
    useAutomationWorkflowProjectVersionsQuery: () => ({
        data: {
            automationWorkflowProjectVersions: [
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 1},
                {description: null, publishedDate: null, status: 'DRAFT', version: 2},
                {description: null, publishedDate: null, status: 'PUBLISHED', version: 3},
            ],
        },
    }),
    useCreateAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: mocks.createMutate}),
    useUpdateAutomationWorkflowProjectConfigurationMutation: () => ({isPending: false, mutate: mocks.updateMutate}),
}));

const project = {
    categoryId: null,
    configurationCount: 0,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
    lastPublishedVersion: 3,
    name: 'My Project',
    permissionExpression: null,
    published: true,
    tagIds: [],
    version: 4,
    workflowTemplates: [],
};

const existingConfiguration = {
    automationHubVisible: true,
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    projectId: 'project-1',
    projectVersion: 3,
    workflows: [],
};

describe('AutomationWorkflowProjectConfigurationDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('creates a configuration with the chosen environment and version', async () => {
        const user = userEvent.setup();

        render(<AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />);

        await user.click(screen.getByRole('combobox', {name: 'Environment'}));
        await user.click(await screen.findByRole('option', {name: 'Staging'}));

        await user.click(screen.getByRole('combobox', {name: 'Version'}));
        await user.click(await screen.findByRole('option', {name: 'V3'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.createMutate).toHaveBeenCalledTimes(1));

        expect(mocks.createMutate.mock.calls[0][0]).toEqual({
            automationHubVisible: true,
            enabled: false,
            environmentId: '2',
            projectId: 'project-1',
            projectVersion: 3,
        });
    });

    it('offers only published versions', async () => {
        const user = userEvent.setup();

        render(<AutomationWorkflowProjectConfigurationDialog onClose={vi.fn()} project={project} />);

        await user.click(screen.getByRole('combobox', {name: 'Version'}));

        expect(await screen.findByRole('option', {name: 'V1'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'V3'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'V2'})).not.toBeInTheDocument();
    });

    it('edits an existing configuration with the environment fixed', async () => {
        const user = userEvent.setup();

        const onClose = vi.fn();

        mocks.updateMutate.mockImplementation((_variables, options: {onSuccess: () => void}) => options.onSuccess());

        render(
            <AutomationWorkflowProjectConfigurationDialog
                configuration={existingConfiguration}
                onClose={onClose}
                project={project}
            />
        );

        expect(screen.getByText('Edit Configuration')).toBeInTheDocument();

        const environmentSelect = screen.getByRole('combobox', {name: 'Environment'});

        expect(environmentSelect).toBeDisabled();
        expect(environmentSelect).toHaveTextContent('Staging');

        await user.click(screen.getByRole('combobox', {name: 'Version'}));
        await user.click(await screen.findByRole('option', {name: 'V1'}));

        await user.click(screen.getByRole('switch', {name: 'Enabled'}));
        await user.click(screen.getByRole('switch', {name: 'Show in Automation Hub'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            automationHubVisible: false,
            enabled: false,
            id: 'configuration-1',
            projectVersion: 1,
        });
        expect(mocks.createMutate).not.toHaveBeenCalled();

        await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
    });

    it('warns that moving to an older version disconnects automations it lacks', async () => {
        const user = userEvent.setup();

        const configuration = {
            automationHubVisible: true,
            enabled: true,
            environmentId: '2',
            id: 'configuration-1',
            lastExecutionDate: null,
            lastModifiedDate: null,
            projectId: 'project-1',
            projectVersion: 3,
            workflows: [],
        };

        render(
            <AutomationWorkflowProjectConfigurationDialog
                configuration={configuration}
                onClose={vi.fn()}
                project={project}
            />
        );

        const warningText = 'Versions that lack workflows connected users still use cannot be selected.';

        expect(screen.queryByText(warningText)).not.toBeInTheDocument();

        await user.click(screen.getByRole('combobox', {name: 'Version'}));
        await user.click(await screen.findByRole('option', {name: 'V1'}));

        expect(await screen.findByText(warningText)).toBeInTheDocument();
    });
});
