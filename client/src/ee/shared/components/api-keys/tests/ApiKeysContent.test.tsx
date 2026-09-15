import {ApiKey} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiKeysContent from '../ApiKeysContent';
import ApiKeyTable from '../components/ApiKeyTable';

const hoistedApiKeys = vi.hoisted(() => ({apiKeys: [] as unknown[]}));

vi.mock('@/ee/shared/components/api-keys/hooks/useApiKeys', () => ({
    default: () => ({
        apiKeys: hoistedApiKeys.apiKeys,
        apiKeysError: null,
        apiKeysLoading: false,
        handleDelete: vi.fn(),
        handleSave: vi.fn(),
    }),
}));

vi.mock('@/ee/shared/components/api-keys/components/ApiKeyDialog', () => ({
    default: ({triggerNode}: {triggerNode?: ReactNode}) => <>{triggerNode}</>,
}));

vi.mock('@/ee/shared/components/api-keys/components/ApiKeyDeleteDialog', () => ({
    default: () => null,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right}: {right?: ReactNode}) => <div>{right}</div>,
}));

const API_KEYS = [
    {
        createdBy: 'admin@localhost.com',
        createdDate: 1_700_000_000_000,
        id: '1',
        lastUsedDate: 1_700_000_000_000,
        name: 'Primary key',
        secretKey: 'sk-****',
    },
] as unknown as ApiKey[];

beforeEach(() => {
    hoistedApiKeys.apiKeys = [];

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('ApiKeysContent', () => {
    it('should show New API Key in the empty state by default', () => {
        render(<ApiKeysContent description="description" title="API Keys" />);

        expect(screen.getByRole('button', {name: 'New API Key'})).toBeInTheDocument();
    });

    it('should hide New API Key in the empty state when canCreate is false', () => {
        render(<ApiKeysContent canCreate={false} description="description" title="API Keys" />);

        expect(screen.getByText('No API Keys')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New API Key'})).not.toBeInTheDocument();
    });

    it('should hide the header New API Key and row Delete when neither create nor delete is allowed', () => {
        hoistedApiKeys.apiKeys = API_KEYS;

        render(<ApiKeysContent canCreate={false} canDelete={false} description="description" title="API Keys" />);

        expect(screen.getByText('Primary key')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New API Key'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete API Key'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Edit API Key'})).toBeInTheDocument();
    });

    it('should show the header New API Key and row Delete by default', () => {
        hoistedApiKeys.apiKeys = API_KEYS;

        render(<ApiKeysContent description="description" title="API Keys" />);

        expect(screen.getByRole('button', {name: 'New API Key'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete API Key'})).toBeInTheDocument();
    });
});

describe('ApiKeyTable', () => {
    it('should hide the Delete button when canDelete is false', () => {
        render(<ApiKeyTable apiKeys={API_KEYS} canDelete={false} />);

        expect(screen.getByRole('button', {name: 'Edit API Key'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete API Key'})).not.toBeInTheDocument();
    });
});
