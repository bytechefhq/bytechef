import {Form, FormField, FormItem} from '@/components/ui/form';
import {render, screen} from '@/shared/util/test-utils';
import {useForm} from 'react-hook-form';
import {describe, expect, it} from 'vitest';

import {FormLabelWithDescription} from '../FormLabelWithDescription';

interface FormLabelWithDescriptionHarnessProps {
    description?: string;
    required?: boolean;
}

const FormLabelWithDescriptionHarness = ({description, required}: FormLabelWithDescriptionHarnessProps) => {
    const form = useForm();

    return (
        <Form {...form}>
            <FormField
                control={form.control}
                name="name"
                render={() => (
                    <FormItem>
                        <FormLabelWithDescription description={description} label="Name" required={required} />
                    </FormItem>
                )}
            />
        </Form>
    );
};

describe('FormLabelWithDescription', () => {
    it('should mark a required label without a description with an asterisk', () => {
        render(<FormLabelWithDescriptionHarness required />);

        expect(screen.getByText('Name', {selector: 'label'})).toHaveTextContent(/^Name\*$/);
    });

    it('should not mark an optional label without a description', () => {
        render(<FormLabelWithDescriptionHarness />);

        expect(screen.getByText('Name', {selector: 'label'})).toHaveTextContent(/^Name$/);
    });

    it('should mark a required label with a description with an asterisk', () => {
        render(<FormLabelWithDescriptionHarness description="Your full name" required />);

        expect(screen.getByText('Name', {selector: 'span'})).toHaveTextContent(/^Name\*$/);
        expect(screen.getByLabelText('Field description')).toBeInTheDocument();
    });

    it('should not mark an optional label with a description', () => {
        render(<FormLabelWithDescriptionHarness description="Your full name" />);

        expect(screen.getByText('Name', {selector: 'span'})).toHaveTextContent(/^Name$/);
        expect(screen.getByLabelText('Field description')).toBeInTheDocument();
    });
});
