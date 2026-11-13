import {i18n} from '@lingui/core';
import {I18nProvider} from '@lingui/react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import {CreatableMultiSelectItem, RequiredMultiSelectItem, SelectItem, TextItem} from '../SchemaMenuItem';

i18n.load('en', {});
i18n.activate('en');

const wrapper = ({children}: {children: ReactNode}) => <I18nProvider i18n={i18n}>{children}</I18nProvider>;

describe('SchemaMenuItem', () => {
    it('renders a newly added text field empty with the field placeholder', () => {
        render(
            <TextItem
                field={{label: 'description', placeholder: 'What this pill holds', type: 'text', value: 'description'}}
                onChange={vi.fn()}
                schema={{type: 'string'}}
            />
        );

        expect(screen.getByPlaceholderText('What this pill holds')).toHaveValue('');
    });

    it('shows readable placeholder and empty-menu texts in the creatable options select', async () => {
        render(
            <CreatableMultiSelectItem
                field={{label: 'options', type: 'multi_creatable', value: 'enum'}}
                onChange={vi.fn()}
                schema={{type: 'string'}}
            />,
            {wrapper}
        );

        expect(screen.getByText('Options')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByText('No options')).toBeInTheDocument();
    });

    it('shows readable placeholder and empty-menu texts in the single select', async () => {
        render(
            <SelectItem
                field={{label: 'format', optionList: [], type: 'select', value: 'format'}}
                onChange={vi.fn()}
                schema={{type: 'string'}}
            />,
            {wrapper}
        );

        expect(screen.getAllByText('Options').length).toBeGreaterThan(0);

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByText('No options')).toBeInTheDocument();
    });

    it('shows readable placeholder and empty-menu texts in the required properties select', async () => {
        render(
            <RequiredMultiSelectItem
                field={{label: 'required', type: 'required', value: 'required'}}
                onChange={vi.fn()}
                schema={{properties: {name: {type: 'string'}}, required: ['name'], type: 'object'}}
            />,
            {wrapper}
        );

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByText('No options')).toBeInTheDocument();
    });
});
