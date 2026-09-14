import {Type} from '@/pages/automation/mcp-servers/McpServers';
import McpServersFilterTitle from '@/pages/automation/mcp-servers/components/McpServersFilterTitle';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/automation/mcp-servers/McpServers', () => ({
    Type: {Component: 0, Project: 1, Tag: 2},
}));

const renderTitle = (url: string, filterData: {id?: string; type: Type}) =>
    render(
        <MemoryRouter initialEntries={[url]}>
            <McpServersFilterTitle
                componentDefinitions={[{name: 'httpClient', title: 'HTTP Client'} as never]}
                filterData={filterData}
                tags={[]}
                uniqueProjects={[{id: '7', name: 'AI Agent'}]}
            />
        </MemoryRouter>
    );

describe('McpServersFilterTitle', () => {
    it('shows all components when no filter is selected', () => {
        renderTitle('/automation/mcp-servers', {type: Type.Component});

        expect(screen.getByText('Components: All Components')).toBeInTheDocument();
        expect(screen.queryByText('none')).not.toBeInTheDocument();
    });

    it('shows the selected component', () => {
        renderTitle('/automation/mcp-servers?componentName=httpClient', {id: 'httpClient', type: Type.Component});

        expect(screen.getByText('Components: HTTP Client')).toBeInTheDocument();
    });

    it('shows the selected project', () => {
        renderTitle('/automation/mcp-servers?projectId=7', {id: '7', type: Type.Project});

        expect(screen.getByText('Projects: AI Agent')).toBeInTheDocument();
    });
});
