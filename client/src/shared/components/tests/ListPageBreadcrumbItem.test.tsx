import {Breadcrumb, BreadcrumbList} from '@/components/ui/breadcrumb';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {expect, it} from 'vitest';

it('links to the list page and is followed by a separator', () => {
    render(
        <MemoryRouter>
            <Breadcrumb>
                <BreadcrumbList>
                    <ListPageBreadcrumbItem label="Projects" to="/automation/projects" />
                </BreadcrumbList>
            </Breadcrumb>
        </MemoryRouter>
    );

    expect(screen.getByRole('link', {name: 'Projects'})).toHaveAttribute('href', '/automation/projects');
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByRole('presentation', {hidden: true})).toBeInTheDocument();
});
