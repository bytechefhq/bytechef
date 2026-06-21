import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import FieldMappingField from './FieldMappingField';
import {BoundFieldMappingConfigType} from './types';

const config: BoundFieldMappingConfigType = {
    applicationFields: {
        fields: [
            {label: 'Title', value: 'title'},
            {label: 'Email', value: 'email'},
        ],
    },
    integrationFields: {
        get: vi.fn().mockResolvedValue([
            {label: 'First Name', value: 'first_name'},
            {label: 'Subject', value: 'subject'},
        ]),
    },
    objectTypes: {
        get: vi.fn().mockResolvedValue([
            {label: 'Contacts', value: 'contacts'},
            {label: 'Leads', value: 'leads'},
        ]),
    },
};

describe('FieldMappingField', () => {
    it('loads object types and renders an application-field row per configured field', async () => {
        render(<FieldMappingField config={config} label="Contact Mapping" onChange={vi.fn()} value={undefined} />);

        await waitFor(() => expect(screen.getByRole('option', {name: 'Contacts'})).toBeInTheDocument());

        expect(screen.getByText('Title')).toBeInTheDocument();
        expect(screen.getByText('Email')).toBeInTheDocument();
    });

    it('loads integration fields after an object type is chosen and emits the mapping', async () => {
        const onChange = vi.fn();
        const user = userEvent.setup();

        render(<FieldMappingField config={config} label="Contact Mapping" onChange={onChange} value={undefined} />);

        await waitFor(() => expect(screen.getByRole('option', {name: 'Contacts'})).toBeInTheDocument());

        const objectSelect = screen.getByLabelText('Object type', {exact: false});

        await act(async () => {
            await user.selectOptions(objectSelect, 'contacts');
        });

        await waitFor(() => expect(config.integrationFields.get).toHaveBeenCalled());

        const titleRowSelect = screen.getByLabelText('Title', {exact: false});

        await act(async () => {
            await user.selectOptions(titleRowSelect, 'subject');
        });

        expect(onChange).toHaveBeenLastCalledWith({
            mappings: [
                {applicationField: {custom: false, label: 'Title', value: 'title'}, integrationField: 'subject'},
            ],
            objectType: 'contacts',
        });
    });
});
