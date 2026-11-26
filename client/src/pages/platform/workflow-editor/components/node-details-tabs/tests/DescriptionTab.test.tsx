import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import DescriptionTab from '../DescriptionTab';

describe('DescriptionTab', () => {
    it('gives its labels the same line height as the property labels so the fields line up across tabs', () => {
        render(
            <DescriptionTab
                nodeDefinition={{name: 'airtable', title: 'Airtable'} as ComponentDefinition}
                updateWorkflowMutation={{} as UpdateWorkflowMutationType}
            />
        );

        expect(screen.getByText('Component')).toHaveClass('leading-normal');
        expect(screen.getByText('Title')).toHaveClass('leading-normal');
        expect(screen.getByText('Notes')).toHaveClass('leading-normal');
    });
});
