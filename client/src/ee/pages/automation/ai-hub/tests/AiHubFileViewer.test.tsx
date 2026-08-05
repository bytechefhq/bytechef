import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AiHubFileViewer from '../AiHubFileViewer';

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
}));

// useGetAssetFileQuery is invoked inside the viewer to surface format/metadataJson for the chart-mode dispatch.
// Stubbing it here keeps the existing tests free of QueryClientProvider scaffolding — the format-aware paths get
// their own targeted tests in the chart-format describe block below.
vi.mock('@/shared/middleware/graphql', () => ({
    useGetAssetFileQuery: () => ({data: undefined, error: null, isLoading: false}),
}));

vi.mock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
    default: () => ({content: '# Hello\n\nThis is **markdown**.', loading: false, mimeType: 'text/markdown'}),
}));

describe('AiHubFileViewer', () => {
    it('renders monaco editor in editor mode', () => {
        render(<AiHubFileViewer fileId="1" name="spec.md" viewMode="editor" />);

        expect(screen.getByTestId('monaco-editor')).toHaveTextContent('# Hello');
    });

    it('renders markdown preview in preview mode for .md', () => {
        render(<AiHubFileViewer fileId="1" name="spec.md" viewMode="preview" />);

        expect(screen.getByRole('heading', {level: 1, name: 'Hello'})).toBeInTheDocument();
        expect(screen.getByText('markdown')).toBeInTheDocument();
    });

    it('renders both panes in split mode', () => {
        render(<AiHubFileViewer fileId="1" name="spec.md" viewMode="split" />);

        expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
        expect(screen.getByRole('heading', {level: 1, name: 'Hello'})).toBeInTheDocument();
    });
});

describe('AiHubFileViewer with unknown binary mime type', () => {
    it('renders an iframe with the content endpoint plus a download fallback', async () => {
        // For unknown binaries the renderer hands off to an iframe pointed at the same content endpoint
        // so the browser's built-in viewer takes over for any MIME type it knows (PDF, video, audio).
        // For truly unrecognised types the iframe goes blank and the user falls back to the Download link.
        vi.resetModules();

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({content: '', loading: false, mimeType: 'application/octet-stream'}),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: ViewerWithBinary} = await import('../AiHubFileViewer');

        render(<ViewerWithBinary fileId="2" name="archive.zip" viewMode="preview" />);

        const iframe = screen.getByTitle('archive.zip');

        expect(iframe.tagName).toBe('IFRAME');
        expect(iframe).toHaveAttribute('src', '/api/automation/internal/asset-files/2/content');

        // Download fallback for types the iframe can't render inline.
        expect(screen.getByRole('link', {name: /download/i})).toBeInTheDocument();
        expect(screen.getByText('archive.zip')).toBeInTheDocument();
    });

    it('renders a borderless iframe for PDFs so the browser PDF viewer takes over', async () => {
        vi.resetModules();

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({content: '', loading: false, mimeType: 'application/pdf'}),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: PdfViewer} = await import('../AiHubFileViewer');

        render(<PdfViewer fileId="3" name="invoice.pdf" viewMode="preview" />);

        const iframe = screen.getByTitle('invoice.pdf');

        expect(iframe.tagName).toBe('IFRAME');
        // `?disposition=inline` is required so the server sends `Content-Disposition: inline` — without it
        // the default `attachment` disposition makes the iframe download the PDF instead of rendering it.
        expect(iframe).toHaveAttribute('src', '/api/automation/internal/asset-files/3/content?disposition=inline');
    });
});

describe('AiHubFileViewer with image mime type', () => {
    it('renders an img element with the correct src and alt', async () => {
        vi.resetModules();

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({content: '', loading: false, mimeType: 'image/png'}),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: ImageViewer} = await import('../AiHubFileViewer');

        render(<ImageViewer fileId="42" name="diagram.png" viewMode="preview" />);

        const img = screen.getByRole('img', {name: 'diagram.png'});

        expect(img).toBeInTheDocument();
        expect(img).toHaveAttribute('src', '/api/automation/internal/asset-files/42/content');
        expect(img).toHaveAttribute('alt', 'diagram.png');
    });
});

describe('AiHubFileViewer with pptx mime type', () => {
    it('renders a download link with the file name and PowerPoint label', async () => {
        vi.resetModules();

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({
                content: '',
                loading: false,
                mimeType: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            }),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: PptxViewer} = await import('../AiHubFileViewer');

        render(<PptxViewer fileId="99" name="slides.pptx" viewMode="preview" />);

        expect(screen.getByText('slides.pptx')).toBeInTheDocument();
        expect(screen.getByText('PowerPoint presentation')).toBeInTheDocument();

        const link = screen.getByRole('link', {name: /Download \/ Open externally/i});

        expect(link).toHaveAttribute('href', '/api/automation/internal/asset-files/99/content');
        expect(link).toHaveAttribute('download', 'slides.pptx');
    });
});

describe('AiHubFileViewer with CHART format', () => {
    it('routes to the chart pane when format=CHART', async () => {
        // Pin the format-aware dispatch: a file whose `format` is CHART must render via the chart pane in
        // preview mode, NOT via the generic JSON pre block. Without this routing the user would see the raw
        // chart spec as text — defeating the purpose of the generator.
        vi.resetModules();

        vi.doMock('@/shared/middleware/graphql', () => ({
            useGetAssetFileQuery: () => ({
                data: {
                    assetFile: {
                        format: 'CHART',
                        metadataJson:
                            '{"type":"bar","title":"Revenue","xKey":"region","yKeys":["revenue"],"data":[{"region":"NA","revenue":100}]}',
                    },
                },
                error: null,
                isLoading: false,
            }),
        }));

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({
                content:
                    '{"type":"bar","title":"Revenue","xKey":"region","yKeys":["revenue"],"data":[{"region":"NA","revenue":100}]}',
                loading: false,
                mimeType: 'application/json',
            }),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        // Recharts depends on ResizeObserver which jsdom does not implement; stub it so the chart container
        // doesn't crash on mount. The actual render assertion below is on the title text — we don't need to
        // reach the SVG layer to pin the dispatch.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (globalThis as any).ResizeObserver = class {
            disconnect() {}
            observe() {}
            unobserve() {}
        };

        const {default: ChartViewer} = await import('../AiHubFileViewer');

        render(<ChartViewer fileId="42" name="rev.chart.json" viewMode="preview" />);

        expect(screen.getByText('Revenue')).toBeInTheDocument();
    });

    it('falls back to file content when metadataJson is empty (legacy chart row)', async () => {
        // Defensive invariant: the chart generator always populates metadataJson, but a hand-edited or
        // pre-slice-6 row could land with metadataJson=null. Pin: the viewer reads from file content in that
        // case rather than rendering an empty pane.
        vi.resetModules();

        vi.doMock('@/shared/middleware/graphql', () => ({
            useGetAssetFileQuery: () => ({
                data: {
                    assetFile: {format: 'CHART', metadataJson: null},
                },
                error: null,
                isLoading: false,
            }),
        }));

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({
                content: '{"type":"line","title":"Legacy","xKey":"day","yKeys":["v"],"data":[{"day":"Mon","v":1}]}',
                loading: false,
                mimeType: 'application/json',
            }),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (globalThis as any).ResizeObserver = class {
            disconnect() {}
            observe() {}
            unobserve() {}
        };

        const {default: ChartViewer} = await import('../AiHubFileViewer');

        render(<ChartViewer fileId="50" name="legacy.chart.json" viewMode="preview" />);

        expect(screen.getByText('Legacy')).toBeInTheDocument();
    });
});

describe('AiHubFileViewer with HTML format', () => {
    it('routes to the html interactive pane when format=HTML', async () => {
        // Pin the format-discriminated dispatch: a file whose `format` is HTML must render via the
        // interactive pane (sandbox="allow-scripts") rather than the generic text/html iframe path
        // (sandbox=""). Without the format check, an AI-generated interactive artifact would render
        // as a static HTML page with scripts blocked — defeating the purpose of the format.
        vi.resetModules();

        vi.doMock('@/shared/middleware/graphql', () => ({
            useGetAssetFileQuery: () => ({
                data: {assetFile: {format: 'HTML', metadataJson: null}},
                error: null,
                isLoading: false,
            }),
        }));

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({
                content: '<!doctype html><html><head></head><body><h1>App</h1></body></html>',
                loading: false,
                mimeType: 'text/html',
            }),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: HtmlViewer} = await import('../AiHubFileViewer');

        render(<HtmlViewer fileId="42" name="widget.html" viewMode="preview" />);

        const iframe = screen.getByTitle('widget.html') as HTMLIFrameElement;

        expect(iframe).toHaveAttribute('sandbox', 'allow-scripts');
    });

    it('falls back to the generic text/html branch when format is null (user-uploaded HTML)', async () => {
        // Trust is conferred by the format column, not the mime type. A user-uploaded HTML file has
        // mime=text/html, format=null and continues to render with sandbox="" (no scripts) so we
        // do not grant scripts to untrusted content.
        vi.resetModules();

        vi.doMock('@/shared/middleware/graphql', () => ({
            useGetAssetFileQuery: () => ({
                data: {assetFile: {format: null, metadataJson: null}},
                error: null,
                isLoading: false,
            }),
        }));

        vi.doMock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
            default: () => ({
                content: '<!doctype html><html><head></head><body>uploaded</body></html>',
                loading: false,
                mimeType: 'text/html',
            }),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: UploadedHtmlViewer} = await import('../AiHubFileViewer');

        render(<UploadedHtmlViewer fileId="100" name="upload.html" viewMode="preview" />);

        const iframe = screen.getByTitle('upload.html') as HTMLIFrameElement;

        expect(iframe).toHaveAttribute('sandbox', '');
    });
});
