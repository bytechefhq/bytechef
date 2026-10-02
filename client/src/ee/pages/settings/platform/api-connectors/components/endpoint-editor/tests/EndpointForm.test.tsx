import {HttpMethod} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import EndpointForm from '../EndpointForm';

vi.mock('../ParameterList', () => ({
    default: () => <div data-testid="parameter-list" />,
}));

vi.mock('../RequestBodyEditor', () => ({
    default: () => <div data-testid="request-body-editor" />,
}));

vi.mock('../ResponseEditor', () => ({
    default: () => <div data-testid="response-editor" />,
}));

const onClose = vi.fn();
const onSave = vi.fn();

const endpoint = {
    description: 'List the orders',
    httpMethod: HttpMethod.Get,
    id: 'e1',
    operationId: 'listOrders',
    parameters: [],
    path: '/orders',
    responses: [],
    summary: 'List orders',
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('EndpointForm', () => {
    describe('add mode', () => {
        it('should render the add title', () => {
            render(<EndpointForm onClose={onClose} onSave={onSave} open />);

            expect(screen.getByRole('heading', {name: 'Add Endpoint'})).toBeInTheDocument();
        });

        it('should label the primary action Add', () => {
            render(<EndpointForm onClose={onClose} onSave={onSave} open />);

            expect(screen.getByRole('button', {name: 'Add'})).toBeInTheDocument();
        });
    });

    describe('edit mode', () => {
        it('should render the edit title', () => {
            render(<EndpointForm endpoint={endpoint} onClose={onClose} onSave={onSave} open />);

            expect(screen.getByRole('heading', {name: 'Edit Endpoint'})).toBeInTheDocument();
        });

        it('should label the primary action Update', () => {
            render(<EndpointForm endpoint={endpoint} onClose={onClose} onSave={onSave} open />);

            expect(screen.getByRole('button', {name: 'Update'})).toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close and cancel controls', () => {
            render(<EndpointForm onClose={onClose} onSave={onSave} open />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<EndpointForm onClose={onClose} onSave={onSave} open />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });

        it('should render the parameter, request body and response editors', () => {
            render(<EndpointForm onClose={onClose} onSave={onSave} open />);

            expect(screen.getByTestId('parameter-list')).toBeInTheDocument();
            expect(screen.getByTestId('request-body-editor')).toBeInTheDocument();
            expect(screen.getByTestId('response-editor')).toBeInTheDocument();
        });

        it('should not render when closed', () => {
            render(<EndpointForm onClose={onClose} onSave={onSave} open={false} />);

            expect(screen.queryByRole('heading', {name: 'Add Endpoint'})).not.toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<EndpointForm onClose={onClose} onSave={onSave} open />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<EndpointForm onClose={onClose} onSave={onSave} open />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
