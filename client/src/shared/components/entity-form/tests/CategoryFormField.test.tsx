import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {FormProvider, useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CategoryFormField from '../CategoryFormField';

// The field reads the form from context, the way the project and integration dialogs provide it.
const FormHarness = ({
    categories,
    categoriesLoading = false,
}: {
    categories?: {name: string}[];
    categoriesLoading?: boolean;
}) => {
    const form = useForm({defaultValues: {category: undefined}});

    return (
        <FormProvider {...form}>
            <CategoryFormField categories={categories} categoriesLoading={categoriesLoading} />
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

describe('CategoryFormField', () => {
    it('should render nothing while the categories are loading', () => {
        render(<FormHarness categoriesLoading />);

        expect(screen.queryByText('Category')).not.toBeInTheDocument();
    });

    it('should label the field once the categories have loaded', () => {
        render(<FormHarness categories={[{name: 'Marketing'}]} />);

        expect(screen.getByText('Category')).toBeInTheDocument();
    });

    // The dialogs pass the query result straight through, which is undefined on the first render after loading flips.
    it('should render without categories', () => {
        render(<FormHarness />);

        expect(screen.getByText('Category')).toBeInTheDocument();
    });
});
