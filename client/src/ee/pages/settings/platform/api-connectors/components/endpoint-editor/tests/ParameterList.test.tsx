import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {FormProvider, useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ParameterList from '../ParameterList';

// ParameterList only ever renders inside EndpointForm's <Form>, and FormLabel
// reads that context, so the harness has to supply it too.
const FormHarness = ({children}: {children: ReactNode}) => {
    const form = useForm();

    return <FormProvider {...form}>{children}</FormProvider>;
};

const onChange = vi.fn();

const parameter = {
    id: 'param-1',
    in: 'query' as const,
    name: 'limit',
    required: true,
    type: 'string' as const,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ParameterList', () => {
    describe('list', () => {
        it('should render the empty state when there are no parameters', () => {
            render(
                <FormHarness>
                    <ParameterList onChange={onChange} parameters={[]} />
                </FormHarness>
            );

            expect(screen.getByText('No parameters defined.')).toBeInTheDocument();
        });

        it('should render the existing parameters', () => {
            render(
                <FormHarness>
                    <ParameterList onChange={onChange} parameters={[parameter]} />
                </FormHarness>
            );

            expect(screen.queryByText('No parameters defined.')).not.toBeInTheDocument();
            expect(screen.getByText('limit')).toBeInTheDocument();
        });

        it('should not show the dialog until it is opened', () => {
            render(
                <FormHarness>
                    <ParameterList onChange={onChange} parameters={[]} />
                </FormHarness>
            );

            expect(screen.queryByText('Add Parameter')).not.toBeInTheDocument();
        });
    });

    describe('add dialog', () => {
        it('should open with the add title', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ParameterList onChange={onChange} parameters={[]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(await screen.findByText('Add Parameter')).toBeInTheDocument();
        });

        it('should render the close and cancel controls', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ParameterList onChange={onChange} parameters={[]} />
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
                    <ParameterList onChange={onChange} parameters={[]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));

            expect(await screen.findByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should close when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ParameterList onChange={onChange} parameters={[]} />
                </FormHarness>
            );

            await user.click(screen.getByRole('button', {name: 'Add'}));
            await user.click(await screen.findByRole('button', {name: 'Cancel'}));

            expect(screen.queryByText('Add Parameter')).not.toBeInTheDocument();
        });
    });

    describe('edit dialog', () => {
        it('should open with the edit title for an existing parameter', async () => {
            const user = userEvent.setup();

            render(
                <FormHarness>
                    <ParameterList onChange={onChange} parameters={[parameter]} />
                </FormHarness>
            );

            await user.click(screen.getByText('limit'));

            expect(await screen.findByText('Edit Parameter')).toBeInTheDocument();
        });
    });
});
