import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {handleConfigureToolMock, handleRemoveToolMock} = vi.hoisted(() => ({
    handleConfigureToolMock: vi.fn(),
    handleRemoveToolMock: vi.fn(),
}));

vi.mock('./hooks/useAiAgentToolDropdownMenu', () => ({
    default: () => ({handleConfigureTool: handleConfigureToolMock, handleRemoveTool: handleRemoveToolMock}),
}));

import AiAgentToolDropdownMenu from './AiAgentToolDropdownMenu';

const httpClientTool = {
    componentName: 'httpClient',
    componentVersion: 1,
    label: 'Get a random quote',
    name: 'httpClient_1',
    operationName: 'get',
    title: 'HTTP Client',
    type: 'httpClient/v1/get',
};

const renderMenu = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <AiAgentToolDropdownMenu tool={httpClientTool} />
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe('AiAgentToolDropdownMenu', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('offers Configure and Remove when the editor is editable', async () => {
        renderMenu(false);

        await userEvent.click(screen.getByRole('button', {name: 'Get a random quote tool actions'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Remove'}));

        expect(handleRemoveToolMock).toHaveBeenCalledWith(httpClientTool);
    });

    it('offers only Configure in read-only mode', async () => {
        renderMenu(true);

        await userEvent.click(screen.getByRole('button', {name: 'Get a random quote tool actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Configure'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Remove'})).not.toBeInTheDocument();
    });
});
