import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentDialogComponentSelectionStep from '../McpComponentDialogComponentSelectionStep';

const hoisted = vi.hoisted(() => ({
    setSearchTerm: vi.fn(),
    useMcpComponentDialogComponentSelectionStep: vi.fn(),
}));

vi.mock('../hooks/useMcpComponentDialogComponentSelectionStep', () => ({
    default: hoisted.useMcpComponentDialogComponentSelectionStep,
}));

const integrationComponents = [
    {id: '5', integrationId: '5', name: 'googleMail', title: 'Gmail', version: 1},
    {id: '6', integrationId: '6', name: 'googleMail', title: 'Gmail Code', version: 1},
];

describe('McpComponentDialogComponentSelectionStep', () => {
    beforeEach(() => {
        hoisted.setSearchTerm.mockReset();
        hoisted.useMcpComponentDialogComponentSelectionStep.mockReset();
        hoisted.useMcpComponentDialogComponentSelectionStep.mockReturnValue({
            filteredComponents: integrationComponents,
            isLoadingComponents: false,
            searchTerm: '',
            setSearchTerm: hoisted.setSearchTerm,
        });
    });

    it('lists one card per integration and selects the integration that is clicked', async () => {
        const onComponentSelect = vi.fn();

        render(
            <McpComponentDialogComponentSelectionStep
                componentName="googleMail"
                onComponentSelect={onComponentSelect}
                open
            />
        );

        expect(hoisted.useMcpComponentDialogComponentSelectionStep).toHaveBeenCalledWith(true, 'googleMail');

        await userEvent.click(screen.getByText('Gmail Code'));

        expect(onComponentSelect).toHaveBeenCalledWith(integrationComponents[1]);
    });

    it('passes the search term changes to the hook', async () => {
        render(<McpComponentDialogComponentSelectionStep onComponentSelect={vi.fn()} open />);

        await userEvent.type(screen.getByPlaceholderText('Search components by name...'), 'g');

        expect(hoisted.setSearchTerm).toHaveBeenCalledWith('g');
    });
});
