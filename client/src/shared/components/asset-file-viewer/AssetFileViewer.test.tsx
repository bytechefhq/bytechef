import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AssetFileViewer from './AssetFileViewer';

const {useAssetFileContentMock, useGetAssetFileQueryMock} = vi.hoisted(() => ({
    useAssetFileContentMock: vi.fn(),
    useGetAssetFileQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useGetAssetFileQuery: useGetAssetFileQueryMock,
}));

vi.mock('./useAssetFileContent', () => ({
    default: useAssetFileContentMock,
}));

// Mirrors the shape of an AI-generated build spec: a GFM pipe table plus a task list, neither of
// which is part of plain CommonMark.
const MARKDOWN_CONTENT = `# Spec

| Decision | Choice |
|---|---|
| Handover triggers | Explicit request |
| Auto-release | Never |

- [x] Attach knowledge base
`;

describe('AssetFileViewer markdown preview', () => {
    beforeEach(() => {
        useGetAssetFileQueryMock.mockReturnValue({data: {assetFile: {format: null, metadataJson: null}}});

        useAssetFileContentMock.mockReturnValue({
            content: MARKDOWN_CONTENT,
            loading: false,
            mimeType: 'text/markdown',
        });
    });

    it('renders a GFM pipe table as a table instead of a paragraph of pipes', () => {
        render(<AssetFileViewer fileId="1" name="spec.md" viewMode="preview" />);

        const preview = screen.getByTestId('asset-file-markdown-preview');

        expect(preview.querySelector('table')).not.toBeNull();
        expect(preview.querySelectorAll('tbody tr')).toHaveLength(2);
        expect(screen.getByText('Handover triggers')).toBeInTheDocument();
    });

    it('renders a GFM task list as checkboxes', () => {
        render(<AssetFileViewer fileId="1" name="spec.md" viewMode="preview" />);

        const preview = screen.getByTestId('asset-file-markdown-preview');

        expect(preview.querySelector('input[type="checkbox"]')).not.toBeNull();
    });
});
