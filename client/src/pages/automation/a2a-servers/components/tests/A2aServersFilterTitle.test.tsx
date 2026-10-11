import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import {A2aServersFilterType} from '../../hooks/useA2aServers';
import A2aServersFilterTitle from '../A2aServersFilterTitle';

const tags = [{id: '3', name: 'sales'}];
const uniqueProjects = [{id: '5', name: 'Sales project'}];

describe('A2aServersFilterTitle', () => {
    it('shows all projects when no filter is set', () => {
        render(
            <A2aServersFilterTitle
                filterData={{type: A2aServersFilterType.Project}}
                tags={tags}
                uniqueProjects={uniqueProjects}
            />
        );

        expect(screen.getByText('Projects: All Projects')).toBeInTheDocument();
    });

    it('shows the filtered project', () => {
        render(
            <A2aServersFilterTitle
                filterData={{id: '5', type: A2aServersFilterType.Project}}
                tags={tags}
                uniqueProjects={uniqueProjects}
            />
        );

        expect(screen.getByText('Projects: Sales project')).toBeInTheDocument();
    });

    it('shows the filtered tag', () => {
        render(
            <A2aServersFilterTitle
                filterData={{id: '3', type: A2aServersFilterType.Tag}}
                tags={tags}
                uniqueProjects={uniqueProjects}
            />
        );

        expect(screen.getByText('Tags: sales')).toBeInTheDocument();
    });

    it('shows an unknown project or tag as unknown', () => {
        const {rerender} = render(
            <A2aServersFilterTitle
                filterData={{id: '99', type: A2aServersFilterType.Project}}
                tags={tags}
                uniqueProjects={uniqueProjects}
            />
        );

        expect(screen.getByText('Projects: Unknown')).toBeInTheDocument();

        rerender(
            <A2aServersFilterTitle
                filterData={{id: '99', type: A2aServersFilterType.Tag}}
                tags={tags}
                uniqueProjects={uniqueProjects}
            />
        );

        expect(screen.getByText('Tags: Unknown')).toBeInTheDocument();
    });
});
