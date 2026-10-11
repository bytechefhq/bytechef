import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

import {A2aServersFilterType} from '../../hooks/useA2aServers';
import A2aServersLeftSidebarNav from '../A2aServersLeftSidebarNav';

describe('A2aServersLeftSidebarNav', () => {
    it('links every project and tag to its filter', () => {
        render(
            <MemoryRouter>
                <A2aServersLeftSidebarNav
                    a2aProjectsIsLoading={false}
                    filterData={{type: A2aServersFilterType.Project}}
                    tags={[{id: '3', name: 'sales'}]}
                    tagsIsLoading={false}
                    uniqueProjects={[{id: '5', name: 'Sales project'}]}
                />
            </MemoryRouter>
        );

        expect(screen.getByText('All Projects')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Sales project'})).toHaveAttribute(
            'href',
            expect.stringContaining('projectId=5')
        );
        expect(screen.getByRole('link', {name: 'sales'})).toHaveAttribute('href', expect.stringContaining('tagId=3'));
    });

    it('explains when there are no projects or tags', () => {
        render(
            <MemoryRouter>
                <A2aServersLeftSidebarNav
                    a2aProjectsIsLoading={false}
                    filterData={{type: A2aServersFilterType.Project}}
                    tags={[]}
                    tagsIsLoading={false}
                    uniqueProjects={[]}
                />
            </MemoryRouter>
        );

        expect(screen.getByText('No projects.')).toBeInTheDocument();
        expect(screen.getByText('No defined tags.')).toBeInTheDocument();
    });
});
