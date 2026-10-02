import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {FormProvider, useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import RequestBodyEditor from '../RequestBodyEditor';

// RequestBodyEditor only ever renders inside EndpointForm's <Form>, and
// FormLabel reads that context, so the harness has to supply it too.
const FormHarness = ({children}: {children: ReactNode}) => {
    const form = useForm();

    return <FormProvider {...form}>{children}</FormProvider>;
};

const onChange = vi.fn();

const requestBody = {
    contentType: 'application/json',
    description: 'The order payload',
    required: true,
    schema: '{}',
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('RequestBodyEditor', () => {
    describe('list', () => {
        it('should not show the dialog until it is opened', () => {
            render(
                <FormHarness>
                    <RequestBodyEditor onChange={onChange} />
                </FormHarness>
            );

            expect(screen.queryByText('Add Request Body')).not.toBeInTheDocument();
        });
    });

    describe('add dialog', () => {
        it('should open with the add title when there is no request body', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <RequestBodyEditor onChange={onChange} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(await screen.findByText('Add Request Body')).toBeInTheDocument();
        });

        it('should render the close and cancel controls', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <RequestBodyEditor onChange={onChange} />
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
                    <RequestBodyEditor onChange={onChange} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(await screen.findByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should close when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <RequestBodyEditor onChange={onChange} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));
            await user.click(await screen.findByRole('button', {name: 'Cancel'}));

            expect(screen.queryByText('Add Request Body')).not.toBeInTheDocument();
        });
    });

    describe('edit dialog', () => {
        it('should open with the edit title when a request body exists', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <RequestBodyEditor onChange={onChange} requestBody={requestBody} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Edit request body'}));

            expect(await screen.findByText('Edit Request Body')).toBeInTheDocument();
        });
    });
});
