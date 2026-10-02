import {ApiConnector} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiConnectorEditDialog from '../ApiConnectorEditDialog';

const hoisted = vi.hoisted(() => ({
    importMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useImportOpenApiSpecificationMutation: () => ({mutate: hoisted.importMutate, reset: vi.fn()}),
    };
});

vi.mock('@/ee/pages/settings/platform/api-connectors/components/IconField', () => ({
    default: () => <div data-testid="icon-field" />,
}));

vi.mock('@/ee/pages/settings/platform/api-connectors/components/OpenApiSpecificationField', () => ({
    default: () => <div data-testid="specification-field" />,
}));

const apiConnector = {
    icon: 'icon.svg',
    name: 'My Connector',
    specification: 'openapi: 3.0.0',
} as ApiConnector;

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ApiConnectorEditDialog', () => {
    describe('rendering', () => {
        it('should render the title and description', () => {
            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            expect(screen.getByText('Edit API Connector')).toBeInTheDocument();
            expect(screen.getByText('Update the API connector configuration.')).toBeInTheDocument();
        });

        it('should prefill the name from the connector and keep it disabled', () => {
            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            const nameInput = screen.getByLabelText('Name');

            expect(nameInput).toHaveValue('My Connector');
            expect(nameInput).toBeDisabled();
        });

        it('should render the icon and specification fields', () => {
            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            expect(screen.getByTestId('icon-field')).toBeInTheDocument();
            expect(screen.getByTestId('specification-field')).toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should import the specification on save', async () => {
            const user = userEvent.setup();

            render(<ApiConnectorEditDialog apiConnector={apiConnector} onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.importMutate).toHaveBeenCalledTimes(1);
        });
    });
});
