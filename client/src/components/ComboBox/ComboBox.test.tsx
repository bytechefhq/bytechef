import '@testing-library/jest-dom';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import ComboBox  from './ComboBox';
import { comboBoxItemsMock } from './ComboBox.mock';

describe('PropertyInput', async () => {
    it('should render the input', () => {
        render(
            <ComboBox disabled={false} items={comboBoxItemsMock} name='Test' value={comboBoxItemsMock[0].value} />
        );

        expect(
            screen.getByText('Option 1')
        ).toBeInTheDocument();
    });
});
