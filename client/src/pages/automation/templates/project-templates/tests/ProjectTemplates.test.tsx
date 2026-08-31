import {useTemplatesStore} from '@/pages/automation/templates/stores/useTemplatesStore';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectTemplates from '../ProjectTemplates';

import type {PreBuiltProjectTemplatesQuery} from '@/shared/middleware/graphql';

type ProjectTemplateType = PreBuiltProjectTemplatesQuery['preBuiltProjectTemplates'][number];
type ProjectTemplateComponentType = ProjectTemplateType['components'][number]['value'][number];

const hoisted = vi.hoisted(() => ({
    hasData: true,
    isLoading: false,
    templates: [] as Array<unknown>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    usePreBuiltProjectTemplatesQuery: () => ({
        data: hoisted.hasData ? {preBuiltProjectTemplates: hoisted.templates} : undefined,
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

const makeComponent = (icon: string | null): ProjectTemplateComponentType => ({
    connection: null,
    icon,
    name: 'component',
    title: 'Component',
    version: 1,
});

const makeTemplate = (overrides: Partial<ProjectTemplateType> = {}): ProjectTemplateType => ({
    authorName: 'ByteChef',
    categories: [],
    components: [],
    description: null,
    id: 'template-1',
    project: {description: 'Alpha description', name: 'Alpha Project'},
    projectVersion: 1,
    publicUrl: null,
    workflows: [],
    ...overrides,
});

describe('ProjectTemplates', () => {
    beforeEach(() => {
        hoisted.hasData = true;
        hoisted.isLoading = false;
        hoisted.templates = [];

        useTemplatesStore.setState({category: undefined, query: undefined});
    });

    it('renders skeletons instead of an empty state while loading', () => {
        hoisted.isLoading = true;

        const {container} = render(<ProjectTemplates />);

        expect(container.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(3);
        expect(screen.queryByText('No project templates')).not.toBeInTheDocument();
    });

    it('renders a card for every template', () => {
        hoisted.templates = [
            makeTemplate({id: 'template-1', project: {description: null, name: 'Alpha Project'}}),
            makeTemplate({id: 'template-2', project: {description: null, name: 'Beta Project'}}),
        ];

        render(<ProjectTemplates />);

        expect(screen.getAllByTestId('template-card')).toHaveLength(2);
        expect(screen.getByText('Alpha Project')).toBeInTheDocument();
        expect(screen.getByText('Beta Project')).toBeInTheDocument();
        expect(screen.queryByText('No project templates')).not.toBeInTheDocument();
    });

    it('passes only non-null icons of every component to the card', () => {
        hoisted.templates = [
            makeTemplate({
                components: [
                    {key: 'first', value: [makeComponent('alpha.svg'), makeComponent(null)]},
                    {key: 'second', value: [null, makeComponent('beta.svg')]},
                ],
            }),
        ];

        render(<ProjectTemplates />);

        expect(screen.getByTestId('template-card')).toHaveAttribute('data-icons', 'alpha.svg|beta.svg');
    });

    it('says there is nothing to import when no template is returned', () => {
        render(<ProjectTemplates />);

        expect(screen.getByText('No project templates')).toBeInTheDocument();
        expect(screen.getByText('There are no project templates available to import yet.')).toBeInTheDocument();
    });

    it('shows the empty state when the query resolves without data', () => {
        hoisted.hasData = false;

        render(<ProjectTemplates />);

        expect(screen.getByText('No project templates')).toBeInTheDocument();
    });

    it('says nothing matched when a search query is active', () => {
        useTemplatesStore.setState({query: 'nothing matches this'});

        render(<ProjectTemplates />);

        expect(screen.getByText('No matching project templates')).toBeInTheDocument();
        expect(screen.getByText('Try a different search term or category.')).toBeInTheDocument();
    });

    it('says nothing matched when a category is selected', () => {
        useTemplatesStore.setState({category: 'marketing'});

        render(<ProjectTemplates />);

        expect(screen.getByText('No matching project templates')).toBeInTheDocument();
    });
});
