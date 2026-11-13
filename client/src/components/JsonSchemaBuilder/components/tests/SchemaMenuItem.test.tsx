import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import {TextItem} from '../SchemaMenuItem';

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
});
