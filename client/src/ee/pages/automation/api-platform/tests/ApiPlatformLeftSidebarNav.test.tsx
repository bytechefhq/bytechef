import {Type} from '@/ee/pages/automation/api-platform/api-collections/ApiCollections';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import ApiPlatformLeftSidebarNav from '../ApiPlatformLeftSidebarNav';

const PROJECTS = [
    {id: 1, name: 'Billing'},
    {id: 2, name: 'Onboarding'},
];

const TAGS = [
    {id: 10, name: 'api'},
    {id: 11, name: 'internal'},
];

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

const renderNav = (props: Partial<Parameters<typeof ApiPlatformLeftSidebarNav>[0]> = {}) =>
    render(
        <MemoryRouter>
            <ApiPlatformLeftSidebarNav filterData={{type: Type.Project}} projects={PROJECTS} tags={TAGS} {...props} />
        </MemoryRouter>
    );

describe('ApiPlatformLeftSidebarNav', () => {
    it('renders both groups once loaded, with no placeholders left behind', () => {
        renderNav();

        expect(screen.getByText('Billing')).toBeInTheDocument();
        expect(screen.getByText('api')).toBeInTheDocument();
        expect(screen.queryByTestId('left-sidebar-nav-skeleton')).not.toBeInTheDocument();
    });

    it('shows placeholders in each group while both queries are in flight', () => {
        renderNav({projects: undefined, projectsIsLoading: true, tags: undefined, tagsIsLoading: true});

        expect(screen.getAllByTestId('left-sidebar-nav-skeleton')).toHaveLength(2);
        expect(screen.getByText('Projects')).toBeInTheDocument();
        expect(screen.getByText('Tags')).toBeInTheDocument();
    });

    it('places placeholders only in the group that is still loading', () => {
        renderNav({tags: undefined, tagsIsLoading: true});

        expect(screen.getByText('Billing')).toBeInTheDocument();
        expect(screen.getAllByTestId('left-sidebar-nav-skeleton')).toHaveLength(1);
    });

    it('reports an empty tag list rather than a placeholder once the tags arrive', () => {
        renderNav({tags: []});

        expect(screen.getByText('No defined tags.')).toBeInTheDocument();
        expect(screen.queryByTestId('left-sidebar-nav-skeleton')).not.toBeInTheDocument();
    });
});
