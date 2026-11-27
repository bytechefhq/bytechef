import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationDialog from '../IntegrationDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    createOptions: {current: undefined as {onSuccess?: (integrationId: number) => void} | undefined},
    updateMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/integrations.mutations', () => ({
    useCreateIntegrationMutation: (options: {onSuccess?: (integrationId: number) => void}) => {
        hoisted.createOptions.current = options;

        return {mutate: hoisted.createMutate, reset: vi.fn()};
    },
    useUpdateIntegrationMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {integration: (id: number) => ['integration', id], integrations: ['integrations']},
}));

vi.mock('@/ee/shared/queries/embedded/integrationCategories.queries', () => ({
    IntegrationCategoryKeys: {integrationCategories: ['integrationCategories']},
    useGetIntegrationCategoriesQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationTags.quries', () => ({
    IntegrationTagKeys: {integrationTags: ['integrationTags']},
    useGetIntegrationTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationCreated: vi.fn()}),
}));

const onClose = vi.fn();

const newIntegration = {componentName: 'github', multipleInstances: false} as Integration;

const existingIntegration = {
    componentName: 'github',
    id: 1,
    multipleInstances: false,
    name: 'Existing',
} as Integration;

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IntegrationDialog', () => {
    describe('create mode', () => {
        it('should render the create title and description', () => {
            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            expect(screen.getByText('Create Integration')).toBeInTheDocument();
            expect(screen.getByText('Use this to create an integration your users connect to')).toBeInTheDocument();
        });

        it('should offer both usages with workflows selected by default', () => {
            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            expect(screen.getByRole('radio', {name: 'With workflows'})).toBeChecked();
            expect(screen.getByRole('radio', {name: 'Actions, tools & MCP'})).not.toBeChecked();
        });

        it('should report the chosen usage when the integration is created', async () => {
            const onSuccess = vi.fn();
            const user = userEvent.setup();

            render(<IntegrationDialog integration={newIntegration} onClose={onClose} onSuccess={onSuccess} />);

            await user.click(screen.getByRole('radio', {name: 'Actions, tools & MCP'}));

            act(() => hoisted.createOptions.current?.onSuccess?.(5));

            expect(onSuccess).toHaveBeenCalledWith(5, 'connectionOnly');
        });

        it('should render the component picker', () => {
            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            expect(screen.getByText('Component')).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title', () => {
            render(<IntegrationDialog integration={existingIntegration} onClose={onClose} />);

            expect(screen.getByText('Edit Integration')).toBeInTheDocument();
        });

        it('should not offer a usage choice', () => {
            render(<IntegrationDialog integration={existingIntegration} onClose={onClose} />);

            expect(screen.queryByRole('radio', {name: 'Actions, tools & MCP'})).not.toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should render the name and description fields', () => {
            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            expect(screen.getByText('Name')).toBeInTheDocument();
            expect(screen.getByText('Description')).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<IntegrationDialog integration={newIntegration} onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
