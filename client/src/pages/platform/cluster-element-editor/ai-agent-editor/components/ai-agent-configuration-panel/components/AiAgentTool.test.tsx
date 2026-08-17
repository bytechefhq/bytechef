import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {handleConfigureToolMock, handleRemoveToolMock, useGetComponentDefinitionQueryMock} = vi.hoisted(() => ({
    handleConfigureToolMock: vi.fn(),
    handleRemoveToolMock: vi.fn(),
    useGetComponentDefinitionQueryMock: vi.fn((): {data?: {connection?: object; connectionRequired?: boolean}} => ({
        data: undefined,
    })),
}));

vi.mock('./hooks/useAiAgentToolActions', () => ({
    default: () => ({
        handleConfigureTool: handleConfigureToolMock,
        handleRemoveTool: handleRemoveToolMock,
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: useGetComponentDefinitionQueryMock,
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="tool icon" src={src} />,
}));

import AiAgentTool from './AiAgentTool';
import {ToolItemI} from './hooks/useAiAgentTools';

const baseTool: ToolItemI = {
    clusterElementType: 'tools',
    clusterRoot: false,
    componentName: 'example',
    componentVersion: 1,
    label: 'Example',
    name: 'exampleTool_1',
    operationName: 'doSomething',
    title: 'Example',
    type: 'example/v1/doSomething',
};

const renderTool = (tool: ToolItemI, configuredConnectionKeys: Set<string> = new Set<string>()) =>
    render(
        <TooltipProvider>
            <AiAgentTool configuredConnectionKeys={configuredConnectionKeys} tool={tool} />
        </TooltipProvider>
    );

describe('AiAgentTool', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useGetComponentDefinitionQueryMock.mockReturnValue({data: undefined});
    });

    it('offers configure and remove for a plain tool', () => {
        renderTool(baseTool);

        expect(screen.getByLabelText('Configure tool')).toBeInTheDocument();
        expect(screen.getByLabelText('Remove tool')).toBeInTheDocument();
    });

    it('hides configure for a cluster root tool because the simple editor cannot open its nested canvas', () => {
        renderTool({...baseTool, clusterRoot: true});

        expect(screen.queryByLabelText('Configure tool')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Configure on the advanced canvas')).toBeInTheDocument();
    });

    it('keeps remove available for a cluster root tool so an unwanted one can still be detached', () => {
        renderTool({...baseTool, clusterRoot: true});

        expect(screen.getByLabelText('Remove tool')).toBeInTheDocument();
    });

    it('outlines a tool whose component requires a connection that is not configured', () => {
        useGetComponentDefinitionQueryMock.mockReturnValue({data: {connection: {}, connectionRequired: true}});

        renderTool(baseTool);

        expect(screen.getByText('Example').closest('div.rounded')).toHaveClass('border-red-500');
    });

    it('leaves a tool with a configured required connection unmarked', () => {
        useGetComponentDefinitionQueryMock.mockReturnValue({data: {connection: {}, connectionRequired: true}});

        renderTool(baseTool, new Set([baseTool.name]));

        expect(screen.getByText('Example').closest('div.rounded')).not.toHaveClass('border-red-500');
    });

    it('leaves a tool with an optional connection unmarked', () => {
        useGetComponentDefinitionQueryMock.mockReturnValue({data: {connection: {}, connectionRequired: false}});

        renderTool(baseTool);

        expect(screen.getByText('Example').closest('div.rounded')).not.toHaveClass('border-red-500');
    });
});
