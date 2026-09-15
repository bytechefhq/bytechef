import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useAiAgentModelSelectFieldMock} = vi.hoisted(() => ({
    useAiAgentModelSelectFieldMock: vi.fn(),
}));

vi.mock('./hooks/useAiAgentModelSelectField', () => ({
    default: useAiAgentModelSelectFieldMock,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children}: {children: ReactNode}) => <div data-testid="model-popover">{children}</div>,
}));

import AiAgentModelSelectField from './AiAgentModelSelectField';

const renderField = (readOnly = false) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <TooltipProvider>
                <AiAgentModelSelectField />
            </TooltipProvider>
        </WorkflowEditorReadOnlyContext.Provider>
    );

const openAiModel = {
    componentName: 'openAi',
    label: 'openAi_1',
    name: 'openAi_1',
    operationName: 'model',
    title: 'OpenAI',
    type: 'openAi/v1/model',
};

const mockHook = ({
    isConnectionMissing = false,
    model = null,
}: {
    isConnectionMissing?: boolean;
    model?: typeof openAiModel | null;
}) =>
    useAiAgentModelSelectFieldMock.mockReturnValue({
        handleConfigureModel: vi.fn(),
        isConnectionMissing,
        model,
        rootWorkflowNodeName: 'aiAgent_1',
    });

describe('AiAgentModelSelectField', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('marks the model as required', () => {
        mockHook({});

        renderField();

        expect(screen.getByRole('heading', {name: /model/i})).toHaveTextContent('*');
    });

    it('outlines the empty select in red', () => {
        mockHook({});

        renderField();

        expect(screen.getByRole('button', {name: /select a model/i})).toHaveClass('border-red-500');
    });

    it('leaves a configured model with a connection unmarked', () => {
        mockHook({model: openAiModel});

        renderField();

        expect(screen.getByRole('button', {name: /OpenAI/})).not.toHaveClass('border-red-500');
    });

    it('outlines a configured model whose connection is missing in red', () => {
        mockHook({isConnectionMissing: true, model: openAiModel});

        renderField();

        expect(screen.getByRole('button', {name: /OpenAI/})).toHaveClass('border-red-500');
    });

    it('lets the model be changed when the editor is editable', () => {
        mockHook({model: openAiModel});

        renderField();

        expect(screen.getByTestId('model-popover')).toBeInTheDocument();
    });

    it('shows the model without the change-model popover in read-only mode', () => {
        mockHook({model: openAiModel});

        renderField(true);

        expect(screen.getByRole('button', {name: /OpenAI/})).toBeInTheDocument();
        expect(screen.queryByTestId('model-popover')).not.toBeInTheDocument();
    });
});
