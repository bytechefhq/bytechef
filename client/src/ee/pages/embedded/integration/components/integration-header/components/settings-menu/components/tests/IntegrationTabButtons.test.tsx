import IntegrationTabButtons from '@/ee/pages/embedded/integration/components/integration-header/components/settings-menu/components/IntegrationTabButtons';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const mockProps = {
    onCloseDropdownMenuClick: vi.fn(),
    onDeleteIntegrationClick: vi.fn(),
    onImportWorkflow: vi.fn(),
    onNewWorkflowClick: vi.fn(),
    onShowEditIntegrationDialogClick: vi.fn(),
    onShowIntegrationVersionHistorySheet: vi.fn(),
};

beforeEach(() => {
    vi.clearAllMocks();
});

it('opens the new workflow dialog and closes the menu when New Workflow is clicked', async () => {
    render(<IntegrationTabButtons {...mockProps} />);

    await userEvent.click(screen.getByLabelText('New Workflow'));

    expect(mockProps.onNewWorkflowClick).toHaveBeenCalled();
    expect(mockProps.onCloseDropdownMenuClick).toHaveBeenCalled();
});

it('imports the selected workflow file', async () => {
    const {container} = render(<IntegrationTabButtons {...mockProps} />);

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    await userEvent.upload(fileInput, new File(['{"label": "Imported"}'], 'workflow.json', {type: 'application/json'}));

    expect(mockProps.onImportWorkflow).toHaveBeenCalledWith('{"label": "Imported"}');
});
