import {McpServer} from '@/shared/middleware/graphql';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpAutomationProjectConfigurationList from '../McpAutomationProjectConfigurationList';

const hoisted = vi.hoisted(() => ({
    useMcpAutomationProjectConfigurationList: vi.fn(),
}));

vi.mock('../hooks/useMcpAutomationProjectConfigurationList', () => ({
    default: hoisted.useMcpAutomationProjectConfigurationList,
}));

vi.mock('../McpAutomationProjectConfigurationListItem', () => ({
    default: ({mcpAutomationProjectConfiguration}: {mcpAutomationProjectConfiguration: {id: string}}) => (
        <div data-testid="automation-item">{mcpAutomationProjectConfiguration.id}</div>
    ),
}));

vi.mock('../McpAutomationProjectConfigurationDialog', () => ({default: () => null}));

const mcpServer = {environmentId: '1', id: 's1', name: 'Server'} as McpServer;

describe('McpAutomationProjectConfigurationList', () => {
    beforeEach(() => {
        hoisted.useMcpAutomationProjectConfigurationList.mockReset();
    });

    it('rendersAutomationsSection', () => {
        hoisted.useMcpAutomationProjectConfigurationList.mockReturnValue({
            isLoading: false,
            mcpAutomationProjectConfigurations: [{id: 'm1'}, {id: 'm2'}],
        });

        render(<McpAutomationProjectConfigurationList mcpServer={mcpServer} />);

        expect(screen.getByText('Automations')).toBeInTheDocument();
        expect(screen.getAllByTestId('automation-item')).toHaveLength(2);
        expect(screen.getByRole('button', {name: 'Add Automations'})).toBeInTheDocument();
    });

    it('rendersAutomationsSectionWithEmptyMessage', () => {
        hoisted.useMcpAutomationProjectConfigurationList.mockReturnValue({
            isLoading: false,
            mcpAutomationProjectConfigurations: [],
        });

        render(<McpAutomationProjectConfigurationList mcpServer={mcpServer} />);

        expect(screen.getByText('No automations added to this server.')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add Automations'})).toBeInTheDocument();
    });
});
