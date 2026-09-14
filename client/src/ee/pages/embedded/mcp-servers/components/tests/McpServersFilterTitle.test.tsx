import {Type} from '@/ee/pages/embedded/mcp-servers/McpServers';
import McpServersFilterTitle from '@/ee/pages/embedded/mcp-servers/components/McpServersFilterTitle';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

const renderTitle = (url: string, filterData: {id?: string; type: Type}) =>
    render(
        <MemoryRouter initialEntries={[url]}>
            <McpServersFilterTitle
                componentDefinitions={[{name: 'httpClient', title: 'HTTP Client'} as never]}
                filterData={filterData}
                tags={[]}
                uniqueIntegrations={[{id: '3', name: 'Affinity'}]}
            />
        </MemoryRouter>
    );

describe('McpServersFilterTitle', () => {
    it('shows all components when no filter is selected', () => {
        renderTitle('/embedded/mcp-servers', {type: Type.Component});

        expect(screen.getByText('Components: All Components')).toBeInTheDocument();
        expect(screen.queryByText('none')).not.toBeInTheDocument();
    });

    it('shows the selected component', () => {
        renderTitle('/embedded/mcp-servers?componentName=httpClient', {id: 'httpClient', type: Type.Component});

        expect(screen.getByText('Components: HTTP Client')).toBeInTheDocument();
    });

    it('shows the selected integration', () => {
        renderTitle('/embedded/mcp-servers?integrationId=3', {id: '3', type: Type.Integration});

        expect(screen.getByText('Integrations: Affinity')).toBeInTheDocument();
    });
});
