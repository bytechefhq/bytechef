import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import DataPillPanelBody from '../DataPillPanelBody';

vi.mock('../DataPillPanelBodyInputsItem', () => ({
    default: () => <div data-testid="inputs-item" />,
}));

vi.mock('../DataPillPanelBodyVariablesItem', () => ({
    default: () => <div data-testid="variables-item" />,
}));

vi.mock('../DataPillPanelBodyPropertiesItem', () => ({
    default: () => <div data-testid="properties-item" />,
}));

describe('DataPillPanelBody', () => {
    it('renders no Variables accordion item when the seam reports no variables feature (undefined)', () => {
        render(<DataPillPanelBody dataPillFilterQuery="" operations={[]} variables={undefined} />);

        expect(screen.queryByTestId('variables-item')).not.toBeInTheDocument();
    });

    it('renders the Variables accordion item when the seam returns an empty array', () => {
        render(<DataPillPanelBody dataPillFilterQuery="" operations={[]} variables={[]} />);

        expect(screen.getByTestId('variables-item')).toBeInTheDocument();
    });

    it('renders the Variables accordion item alongside Inputs when both are present', () => {
        render(
            <DataPillPanelBody
                dataPillFilterQuery=""
                operations={[]}
                variables={[{id: '1', name: 'API_URL', value: 'x'}]}
                workflowInputs={[{name: 'customer'}]}
            />
        );

        expect(screen.getByTestId('inputs-item')).toBeInTheDocument();
        expect(screen.getByTestId('variables-item')).toBeInTheDocument();
    });

    it('does not render Variables when the seam is undefined even if other data pills exist', () => {
        render(
            <DataPillPanelBody
                dataPillFilterQuery=""
                operations={[]}
                variables={undefined}
                workflowInputs={[{name: 'customer'}]}
            />
        );

        expect(screen.getByTestId('inputs-item')).toBeInTheDocument();
        expect(screen.queryByTestId('variables-item')).not.toBeInTheDocument();
    });

    it('shows the no-data-pills placeholder when there are no inputs, operations, or variables feature', () => {
        render(<DataPillPanelBody dataPillFilterQuery="" operations={[]} variables={undefined} />);

        expect(screen.getByText('No data pills available')).toBeInTheDocument();
    });
});
