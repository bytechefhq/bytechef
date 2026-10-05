import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {FormProvider, useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ResponseEditor from '../ResponseEditor';

// ResponseEditor only ever renders inside EndpointForm's <Form>, and FormLabel
// reads that context, so the harness has to supply it too.
const FormHarness = ({children}: {children: ReactNode}) => {
    const form = useForm();

    return <FormProvider {...form}>{children}</FormProvider>;
};

const onChange = vi.fn();

const response = {
    contentType: 'application/json',
    description: 'Successful response',
    statusCode: '200',
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ResponseEditor', () => {
    describe('list', () => {
        it('should not show the dialog until it is opened', () => {
            render(
                <FormHarness>
                    <ResponseEditor onChange={onChange} responses={[]} />
                </FormHarness>
            );

            expect(screen.queryByText('Add Response')).not.toBeInTheDocument();
        });

        it('should render the existing responses', () => {
            render(
                <FormHarness>
                    <ResponseEditor onChange={onChange} responses={[response]} />
                </FormHarness>
            );

            expect(screen.getByText('200')).toBeInTheDocument();
        });
    });

    describe('add dialog', () => {
        it('should open with the add title', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ResponseEditor onChange={onChange} responses={[]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(await screen.findByText('Add Response')).toBeInTheDocument();
        });

        it('should render the close and cancel controls', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ResponseEditor onChange={onChange} responses={[]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(await screen.findByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ResponseEditor onChange={onChange} responses={[]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(await screen.findByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should close when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ResponseEditor onChange={onChange} responses={[]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));
            await user.click(await screen.findByRole('button', {name: 'Cancel'}));

            expect(screen.queryByText('Add Response')).not.toBeInTheDocument();
        });
    });

    describe('edit dialog', () => {
        // Only the add path was covered, so the editing half of the dialog title never ran.
        it('should open with the edit title when a response row is clicked', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ResponseEditor onChange={onChange} responses={[response]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: /200/}));

            expect(await screen.findByText('Edit Response')).toBeInTheDocument();
        });
    });
});
