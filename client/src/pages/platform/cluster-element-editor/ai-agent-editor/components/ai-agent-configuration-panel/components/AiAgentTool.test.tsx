import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

const {handleConfigureToolMock, handleRemoveToolMock} = vi.hoisted(() => ({
    handleConfigureToolMock: vi.fn(),
    handleRemoveToolMock: vi.fn(),
}));

vi.mock('./hooks/useAiAgentToolActions', () => ({
    default: () => ({
        handleConfigureTool: handleConfigureToolMock,
        handleRemoveTool: handleRemoveToolMock,
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="tool icon" src={src} />,
}));

import AiAgentTool from './AiAgentTool';
import {ToolItemI} from './hooks/useAiAgentTools';

const baseTool: ToolItemI = {
    clusterRoot: false,
    componentName: 'example',
    componentVersion: 1,
    label: 'Example',
    name: 'exampleTool_1',
    operationName: 'doSomething',
    title: 'Example',
    type: 'example/v1/doSomething',
};

const renderTool = (tool: ToolItemI) =>
    render(
        <TooltipProvider>
            <AiAgentTool configuredConnectionKeys={new Set<string>()} tool={tool} />
        </TooltipProvider>
    );

describe('AiAgentTool', () => {
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
});
