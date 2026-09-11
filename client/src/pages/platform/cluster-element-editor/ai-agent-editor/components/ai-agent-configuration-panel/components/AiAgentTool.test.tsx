import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useGetComponentDefinitionQueryMock} = vi.hoisted(() => ({
    useGetComponentDefinitionQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: useGetComponentDefinitionQueryMock,
}));

vi.mock('./AiAgentToolDropdownMenu', () => ({
    default: () => <div data-testid="tool-menu" />,
}));

import AiAgentTool from './AiAgentTool';

const httpClientTool = {
    componentName: 'httpClient',
    componentVersion: 1,
    label: 'Get a random quote',
    name: 'httpClient_1',
    operationName: 'get',
    title: 'HTTP Client',
    type: 'httpClient/v1/get',
};

const renderTool = (configuredConnectionKeys: Set<string> = new Set()) =>
    render(<AiAgentTool configuredConnectionKeys={configuredConnectionKeys} tool={httpClientTool} />);

describe('AiAgentTool', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('outlines a tool whose component requires a connection that is not configured', () => {
        useGetComponentDefinitionQueryMock.mockReturnValue({data: {connection: {}, connectionRequired: true}});

        renderTool();

        expect(screen.getByText('Get a random quote').closest('div.rounded')).toHaveClass('border-red-500');
    });

    it('leaves a tool with a configured required connection unmarked', () => {
        useGetComponentDefinitionQueryMock.mockReturnValue({data: {connection: {}, connectionRequired: true}});

        renderTool(new Set(['httpClient_1']));

        expect(screen.getByText('Get a random quote').closest('div.rounded')).not.toHaveClass('border-red-500');
    });

    it('leaves a tool with an optional connection unmarked', () => {
        useGetComponentDefinitionQueryMock.mockReturnValue({data: {connection: {}, connectionRequired: false}});

        renderTool();

        expect(screen.getByText('Get a random quote').closest('div.rounded')).not.toHaveClass('border-red-500');
    });
});
