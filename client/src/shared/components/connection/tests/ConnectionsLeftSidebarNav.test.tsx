import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it} from 'vitest';

import ConnectionsLeftSidebarNav from '../ConnectionsLeftSidebarNav';

const componentDefinitions = [{name: 'gmail', title: 'Gmail'}] as ComponentDefinitionBasic[];

const renderNav = (preservedSearchParams?: Record<string, string>) =>
    render(
        <MemoryRouter>
            <ConnectionsLeftSidebarNav
                componentDefinitions={componentDefinitions}
                connections={[{componentName: 'gmail'}]}
                preservedSearchParams={preservedSearchParams}
                tags={[{id: 3, name: 'sales'}]}
            />
        </MemoryRouter>
    );

const getLinkSearch = (name: string) => {
    const link = screen.getByRole('link', {name});

    return new URLSearchParams(new URL(link.getAttribute('href') ?? '', 'http://localhost').search);
};

describe('ConnectionsLeftSidebarNav', () => {
    afterEach(() => {
        resetAll();
    });

    it('keeps the preserved params in the component link', () => {
        renderNav({connectedUserId: '7'});

        const search = getLinkSearch('Gmail');

        expect(search.get('connectedUserId')).toBe('7');
        expect(search.get('componentName')).toBe('gmail');
    });

    it('keeps the preserved params in the tag link', () => {
        renderNav({connectedUserId: '7'});

        const search = getLinkSearch('sales');

        expect(search.get('connectedUserId')).toBe('7');
        expect(search.get('tagId')).toBe('3');
    });

    it('keeps only the preserved params in the All Components link', () => {
        renderNav({connectedUserId: '7'});

        expect(getLinkSearch('All Components').toString()).toBe('connectedUserId=7');
    });

    it('builds plain filter links without preserved params', () => {
        renderNav();

        expect(getLinkSearch('Gmail').toString()).toBe('componentName=gmail');
        expect(getLinkSearch('sales').toString()).toBe('tagId=3');
    });
});
