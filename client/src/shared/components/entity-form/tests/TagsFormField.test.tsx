import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {FormProvider, useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import TagsFormField from '../TagsFormField';

// The field reads the form from context, the way the project and integration dialogs provide it.
const FormHarness = ({remainingTags}: {remainingTags?: {name: string}[]}) => {
    const form = useForm({defaultValues: {tags: []}});

    return (
        <FormProvider {...form}>
            <TagsFormField remainingTags={remainingTags} />
        </FormProvider>
    );
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('TagsFormField', () => {
    // The dialogs hide the field entirely until the tag query resolves, rather than showing an empty select.
    it('should render nothing until the tags arrive', () => {
        render(<FormHarness />);

        expect(screen.queryByText('Tags')).not.toBeInTheDocument();
    });

    it('should label the field once the tags arrive', () => {
        render(<FormHarness remainingTags={[{name: 'billing'}]} />);

        expect(screen.getByText('Tags')).toBeInTheDocument();
    });

    it('should render with an empty tag list', () => {
        render(<FormHarness remainingTags={[]} />);

        expect(screen.getByText('Tags')).toBeInTheDocument();
    });
});
