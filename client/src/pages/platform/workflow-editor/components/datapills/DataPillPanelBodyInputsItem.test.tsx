import {Accordion, AccordionItem} from '@radix-ui/react-accordion';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import DataPillPanelBodyInputsItem from './DataPillPanelBodyInputsItem';

// Faithful minimal mock: renders ONLY property.name — no child-expansion logic.
// This mirrors the real DataPill root-branch behaviour (which does NOT expand sub-properties).
// If DataPillPanelBodyInputsItem reverted to passing `properties` to the root pill, 'title'
// would NOT appear here because this mock never iterates property.properties — proving the
// component (not the mock) is responsible for emitting each child pill.
vi.mock('./DataPill', () => ({
    default: ({property}: {property?: {name?: string}}) => <div data-testid="data-pill">{property?.name}</div>,
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationQuery: () => ({
        data: {
            inputs: {
                contactMapping: JSON.stringify({
                    Contacts: {applicationFields: {fields: [{label: 'Title', value: 'title'}]}},
                }),
            },
        },
    }),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: {workflow: unknown}) => unknown) =>
        selector({
            workflow: {
                id: 'w1',
                inputs: [
                    {name: 'contactMapping', type: 'field_mapping'},
                    {name: 'apiKey', type: 'string'},
                ],
            },
        }),
}));

describe('DataPillPanelBodyInputsItem', () => {
    it('renders a root pill and a child pill per applicationFields entry for a field_mapping input', () => {
        render(
            <Accordion collapsible defaultValue="inputs" type="single">
                <AccordionItem value="inputs">
                    <DataPillPanelBodyInputsItem dataPillFilterQuery="" />
                </AccordionItem>
            </Accordion>
        );

        // Root pill renders the input name.
        expect(screen.getByText('contactMapping')).toBeInTheDocument();

        // The component renders a separate child DataPill for each applicationField entry.
        // Because the mock renders only property.name (no properties expansion), 'title'
        // appears only if the component passes a child DataPill with property.name === 'title'.
        expect(screen.getByText('title')).toBeInTheDocument();

        // Three pills total: contactMapping root + title child + apiKey root.
        expect(screen.getAllByTestId('data-pill')).toHaveLength(3);
    });

    it('filters inputs by the data pill filter query', () => {
        render(
            <Accordion collapsible defaultValue="inputs" type="single">
                <AccordionItem value="inputs">
                    <DataPillPanelBodyInputsItem dataPillFilterQuery="api" />
                </AccordionItem>
            </Accordion>
        );

        expect(screen.getByText('apiKey')).toBeInTheDocument();
        expect(screen.queryByText('contactMapping')).not.toBeInTheDocument();
    });
});
