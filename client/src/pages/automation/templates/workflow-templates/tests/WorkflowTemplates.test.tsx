import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowTemplates from '../WorkflowTemplates';

import type {PreBuiltWorkflowTemplatesQuery} from '@/shared/middleware/graphql';

type WorkflowTemplateType = PreBuiltWorkflowTemplatesQuery['preBuiltWorkflowTemplates'][number];
type WorkflowTemplateComponentType = WorkflowTemplateType['components'][number];

const hoisted = vi.hoisted(() => ({
    hasData: true,
    isLoading: false,
    templates: [] as Array<unknown>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    usePreBuiltWorkflowTemplatesQuery: () => ({
        data: hoisted.hasData ? {preBuiltWorkflowTemplates: hoisted.templates} : undefined,
        error: null,
        isLoading: hoisted.isLoading,
    }),
}));

vi.mock('@/pages/automation/templates/components/TemplateCard', () => ({
    TemplateCard: ({icons, title}: {icons: Array<string>; title: string}) => (
        <div data-icons={icons.join('|')} data-testid="template-card">
            {title}
        </div>
    ),
}));

const makeComponent = (icon: string | null): WorkflowTemplateComponentType => ({
    connection: null,
    icon,
    name: 'component',
    title: 'Component',
    version: 1,
});

const makeTemplate = (overrides: Partial<WorkflowTemplateType> = {}): WorkflowTemplateType => ({
    authorName: 'ByteChef',
    categories: [],
    components: [],
    description: null,
    id: 'template-1',
    projectVersion: 1,
    publicUrl: null,
    workflow: {description: 'Alpha description', label: 'Alpha Workflow'},
    ...overrides,
});

describe('WorkflowTemplates', () => {
    beforeEach(() => {
        hoisted.hasData = true;
        hoisted.isLoading = false;
        hoisted.templates = [];

        useTemplatesStore.setState({category: undefined, query: undefined});
    });

    it('renders skeletons instead of an empty state while loading', () => {
        hoisted.isLoading = true;

        const {container} = render(<WorkflowTemplates />);

        expect(container.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(3);
        expect(screen.queryByText('No workflow templates')).not.toBeInTheDocument();
    });

    it('renders a card for every template', () => {
        hoisted.templates = [
            makeTemplate({id: 'template-1', workflow: {description: null, label: 'Alpha Workflow'}}),
            makeTemplate({id: 'template-2', workflow: {description: null, label: 'Beta Workflow'}}),
        ];

        render(<WorkflowTemplates />);

        expect(screen.getAllByTestId('template-card')).toHaveLength(2);
        expect(screen.getByText('Alpha Workflow')).toBeInTheDocument();
        expect(screen.getByText('Beta Workflow')).toBeInTheDocument();
        expect(screen.queryByText('No workflow templates')).not.toBeInTheDocument();
    });

    it('passes only non-null icons to the card', () => {
        hoisted.templates = [
            makeTemplate({
                components: [makeComponent('alpha.svg'), makeComponent(null), makeComponent('beta.svg')],
            }),
        ];

        render(<WorkflowTemplates />);

        expect(screen.getByTestId('template-card')).toHaveAttribute('data-icons', 'alpha.svg|beta.svg');
    });

    it('says there is nothing to import when no template is returned', () => {
        render(<WorkflowTemplates />);

        expect(screen.getByText('No workflow templates')).toBeInTheDocument();
        expect(screen.getByText('There are no workflow templates available to import yet.')).toBeInTheDocument();
    });

    it('shows the empty state when the query resolves without data', () => {
        hoisted.hasData = false;

        render(<WorkflowTemplates />);

        expect(screen.getByText('No workflow templates')).toBeInTheDocument();
    });

    it('says nothing matched when a search query is active', () => {
        useTemplatesStore.setState({query: 'nothing matches this'});

        render(<WorkflowTemplates />);

        expect(screen.getByText('No matching workflow templates')).toBeInTheDocument();
        expect(screen.getByText('Try a different search term or category.')).toBeInTheDocument();
    });

    it('says nothing matched when a category is selected', () => {
        useTemplatesStore.setState({category: 'marketing'});

        render(<WorkflowTemplates />);

        expect(screen.getByText('No matching workflow templates')).toBeInTheDocument();
    });
});
