import {Accordion, AccordionItem} from '@radix-ui/react-accordion';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import DataPillPanelBodyInputsItem from './DataPillPanelBodyInputsItem';

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
    it('renders a single root pill for a field_mapping input and no synthetic child pills', () => {
        render(
            <Accordion collapsible defaultValue="inputs" type="single">
                <AccordionItem value="inputs">
                    <DataPillPanelBodyInputsItem />
                </AccordionItem>
            </Accordion>
        );

        expect(screen.getByText('contactMapping')).toBeInTheDocument();

        expect(screen.queryByText('title')).not.toBeInTheDocument();

        expect(screen.getAllByTestId('data-pill')).toHaveLength(2);
    });
});
