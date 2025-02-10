import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties1/components/property-mentions-input/PropertyMentionsInput';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, test, vi} from 'vitest';

describe('PropertyMentionsInput', () => {
    test('renders without errors', () => {
        const mockFunction = vi.fn();

        render(
            <PropertyMentionsInput
                controlType="TEXT"
                defaultValue=""
                handleInputTypeSwitchButtonClick={mockFunction}
                label="PropertyMentionsInput Label"
                leadingIcon="📄"
                onChange={mockFunction}
                placeholder=""
                type="STRING"
                value=""
            />
        );

        expect(screen.getByText('PropertyMentionsInput Label')).toBeInTheDocument();
    });
});
