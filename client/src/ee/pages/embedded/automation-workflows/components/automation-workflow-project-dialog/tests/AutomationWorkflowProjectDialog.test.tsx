import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectDialog from '../AutomationWorkflowProjectDialog';

const hoisted = vi.hoisted(() => ({grantedScopes: ['INTEGRATION_CREATE', 'INTEGRATION_SETTINGS'] as string[]}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', () => ({
    UNION_ENVIRONMENT_ID: -1,
    useEmbeddedScopeState: () => ({editionUnknown: false, error: false, granted: true, loading: false}),
    useHasEmbeddedScope: (scope: string) => hoisted.grantedScopes.includes(scope),
}));

const existingProject = {
    automationHubVisible: true,
    categoryId: null,
    description: null,
    id: '1',
    lastPublishedVersion: null,
    name: 'Existing',
    permissionExpression: null,
    published: false,
    tagIds: [],
    version: 1,
    workflowTemplates: [],
};

const onClose = vi.fn();
const onSubmit = vi.fn();

const defaultProps = {
    categories: [{id: '1', name: 'Marketing'}],
    onClose,
    onSubmit,
    tags: [{id: '1', name: 'crm'}],
};

beforeEach(() => {
    hoisted.grantedScopes = ['INTEGRATION_CREATE', 'INTEGRATION_SETTINGS'];

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AutomationWorkflowProjectDialog', () => {
    describe('create mode', () => {
        it('should render the create title and description', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByText('Create Project')).toBeInTheDocument();
            expect(screen.getByText('Use this to create a project which will contain workflows')).toBeInTheDocument();
        });

        it('should render empty name and description fields', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByLabelText('Name')).toHaveValue('');
            expect(screen.getByLabelText('Description')).toHaveValue('');
        });
    });

    describe('visibility switch and permission expression gating', () => {
        it('shows the Automation Hub switch on create without settings but hides the permission expression', () => {
            hoisted.grantedScopes = ['INTEGRATION_CREATE'];

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByText('Show in Automation Hub')).toBeInTheDocument();
            expect(screen.queryByText('Permission Expression')).not.toBeInTheDocument();
        });

        it('hides the Automation Hub switch on edit without settings', () => {
            hoisted.grantedScopes = [];

            render(<AutomationWorkflowProjectDialog {...defaultProps} project={existingProject} />);

            expect(screen.queryByText('Show in Automation Hub')).not.toBeInTheDocument();
            expect(screen.queryByText('Permission Expression')).not.toBeInTheDocument();
        });

        it('shows both on edit with settings', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} project={existingProject} />);

            expect(screen.getByText('Show in Automation Hub')).toBeInTheDocument();
            expect(screen.getByText('Permission Expression')).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title and description', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} project={existingProject} />);

            expect(screen.getByText('Edit Project')).toBeInTheDocument();
            expect(screen.getByText('Use this to edit a project which will contain workflows')).toBeInTheDocument();
        });

        it('should prefill the name from the project', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} project={existingProject} />);

            expect(screen.getByLabelText('Name')).toHaveValue('Existing');
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should render the category and tags fields', () => {
            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            expect(screen.getByText('Category')).toBeInTheDocument();
            expect(screen.getByText('Tags')).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should submit the entered values', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.type(screen.getByLabelText('Name'), 'New project');
            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({name: 'New project'}));
        });
    });

    describe('automation hub visibility', () => {
        it('should show new projects in the Automation Hub by default', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.type(screen.getByLabelText('Name'), 'New project');
            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({automationHubVisible: true}));
        });

        it('should submit a project hidden from the Automation Hub', async () => {
            const user = userEvent.setup();

            render(<AutomationWorkflowProjectDialog {...defaultProps} />);

            await user.type(screen.getByLabelText('Name'), 'API only');
            await user.click(screen.getByLabelText('Show in Automation Hub'));
            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({automationHubVisible: false}));
        });
    });
});
