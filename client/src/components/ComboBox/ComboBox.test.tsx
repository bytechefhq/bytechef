import '@testing-library/jest-dom';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import ComboBox from './ComboBox';
import {comboBoxItemsMock} from './ComboBox.mock';

const DuplicateNameLabel = ({tag}: {tag: string}) => (
    <span>
        Same Name <span>{tag}</span>
    </span>
);

const duplicateNameItems = [
    {label: <DuplicateNameLabel tag="alpha" />, value: 11},
    {label: <DuplicateNameLabel tag="beta" />, value: 12},
    {label: 'Other', value: 13},
];

describe('ComboBox', async () => {
    it('should render the input', () => {
        render(<ComboBox disabled={false} items={comboBoxItemsMock} name="Test" value={comboBoxItemsMock[0].value} />);

        expect(screen.getByText('Option 1')).toBeInTheDocument();
    });

    it('should select only one of two items that render the same text', async () => {
        render(
            <ComboBox
                items={[
                    {label: <DuplicateNameLabel tag="alpha" />, value: 11},
                    {label: <DuplicateNameLabel tag="alpha" />, value: 12},
                ]}
                name="Test"
            />
        );

        await userEvent.click(screen.getByRole('combobox'));

        const options = await screen.findAllByRole('option');

        await userEvent.hover(options[1]);

        await waitFor(() => {
            expect(options[1]).toHaveAttribute('aria-selected', 'true');
        });

        expect(options[0]).toHaveAttribute('aria-selected', 'false');
    });

    it('should filter items by their rendered text, not by their values', async () => {
        render(<ComboBox items={duplicateNameItems} name="Test" />);

        await userEvent.click(screen.getByRole('combobox'));

        await userEvent.type(screen.getByPlaceholderText('Search...'), 'beta');

        await waitFor(() => {
            expect(screen.getAllByRole('option')).toHaveLength(1);
        });

        expect(screen.getByRole('option')).toHaveTextContent('Same Name beta');

        await userEvent.clear(screen.getByPlaceholderText('Search...'));
        await userEvent.type(screen.getByPlaceholderText('Search...'), '12');

        await waitFor(() => {
            expect(screen.queryAllByRole('option')).toHaveLength(0);
        });
    });
});
