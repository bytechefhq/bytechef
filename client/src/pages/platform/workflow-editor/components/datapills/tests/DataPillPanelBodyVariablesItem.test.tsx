import {TooltipProvider} from '@/components/ui/tooltip';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import DataPillPanelBodyVariablesItem from '../DataPillPanelBodyVariablesItem';

const {useWorkflowVariablesMock} = vi.hoisted(() => ({
    useWorkflowVariablesMock: vi.fn(),
}));

vi.mock('../../../hooks/useWorkflowVariables', () => ({
    default: useWorkflowVariablesMock,
}));

vi.mock('@/pages/home/stores/usePlatformTypeStore', () => ({
    PlatformType: {AUTOMATION: 0, EMBEDDED: 1},
    usePlatformTypeStore: (selector: (state: {currentType: number}) => unknown) => selector({currentType: 0}),
}));

const renderVariablesItem = (dataPillFilterQuery: string) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <Accordion collapsible defaultValue="variables" type="single">
                    <AccordionItem value="variables">
                        <DataPillPanelBodyVariablesItem dataPillFilterQuery={dataPillFilterQuery} />
                    </AccordionItem>
                </Accordion>
            </TooltipProvider>
        </MemoryRouter>
    );

describe('DataPillPanelBodyVariablesItem', () => {
    it('renders nothing when the seam reports no variables feature (undefined)', () => {
        useWorkflowVariablesMock.mockReturnValue(undefined);

        renderVariablesItem('');

        expect(screen.queryByText('Variables')).not.toBeInTheDocument();
        expect(screen.queryByText(/no variables defined/i)).not.toBeInTheDocument();
    });

    it('shows an empty-state message with no variables defined', () => {
        useWorkflowVariablesMock.mockReturnValue([]);

        renderVariablesItem('');

        expect(screen.getByText(/no variables defined/i)).toBeInTheDocument();
    });

    it('renders a pill per variable when the filter query is empty', () => {
        useWorkflowVariablesMock.mockReturnValue([
            {id: '1', name: 'API_URL', value: 'https://example.com'},
            {id: '2', name: 'API_KEY', value: 'secret'},
        ]);

        renderVariablesItem('');

        expect(screen.getByText('API_URL')).toBeInTheDocument();
        expect(screen.getByText('API_KEY')).toBeInTheDocument();
    });

    it('filters the pills by the data pill filter query', () => {
        useWorkflowVariablesMock.mockReturnValue([
            {id: '1', name: 'API_URL', value: 'https://example.com'},
            {id: '2', name: 'RETRY_COUNT', value: '3'},
        ]);

        renderVariablesItem('API');

        expect(screen.getByText('API_URL')).toBeInTheDocument();
        expect(screen.queryByText('RETRY_COUNT')).not.toBeInTheDocument();
    });
});
