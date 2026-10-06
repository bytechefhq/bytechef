import {McpServer} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import McpServerToolCounts from '../McpServerToolCounts';

describe('McpServerToolCounts', () => {
    it('counts the selected tools across all components, not the components', () => {
        const mcpServer = {
            mcpComponents: [{mcpTools: [{id: '1'}, {id: '2'}]}, {mcpTools: [{id: '3'}]}],
        } as McpServer;

        render(<McpServerToolCounts mcpServer={mcpServer} workflowToolCount={2} />);

        expect(screen.getByText('3 component tools')).toBeInTheDocument();
        expect(screen.getByText('2 workflow tools')).toBeInTheDocument();
    });

    it('uses the singular for one tool and counts zero when nothing is selected', () => {
        const {rerender} = render(
            <McpServerToolCounts
                mcpServer={{mcpComponents: [{mcpTools: [{id: '1'}]}]} as McpServer}
                workflowToolCount={1}
            />
        );

        expect(screen.getByText('1 component tool')).toBeInTheDocument();
        expect(screen.getByText('1 workflow tool')).toBeInTheDocument();

        rerender(<McpServerToolCounts mcpServer={{} as McpServer} workflowToolCount={0} />);

        expect(screen.getByText('0 component tools')).toBeInTheDocument();
        expect(screen.getByText('0 workflow tools')).toBeInTheDocument();
    });
});
