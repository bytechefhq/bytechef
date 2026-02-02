import Badge from '@/components/Badge/Badge';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';
import {HttpMethod} from '@/shared/middleware/graphql';
import {useCallback, useMemo, useRef} from 'react';
import {twMerge} from 'tailwind-merge';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {useEndpointDetailPanelStore} from '../stores/useEndpointDetailPanelStore';

const ApiConnectorEndpointDetailPanel = () => {
    const {apiConnectorName, closePanel, isOpen, selectedEndpoint, specification} = useEndpointDetailPanelStore();
    const editorRef = useRef<StandaloneCodeEditorType | null>(null);

    const getMethodBadgeColor = (method?: HttpMethod) => {
        switch (method) {
            case HttpMethod.Get:
                return 'text-content-brand-primary';
            case HttpMethod.Post:
                return 'text-content-success-primary';
            case HttpMethod.Put:
                return 'text-content-warning-primary';
            case HttpMethod.Patch:
                return 'text-orange-700';
            case HttpMethod.Delete:
                return 'text-content-destructive-primary';
            default:
                return '';
        }
    };

    const extractEndpointSpec = useCallback(() => {
        if (!specification || !selectedEndpoint) {
            return null;
        }

        const path = selectedEndpoint.path;
        const method = selectedEndpoint.httpMethod?.toLowerCase();

        if (!path || !method) {
            return specification;
        }

        try {
            const parsedSpec = yamlParse(specification);

            if (!parsedSpec.paths?.[path]?.[method]) {
                return null;
            }

            const endpointSpec = {
                info: parsedSpec.info,
                openapi: parsedSpec.openapi,
                paths: {
                    [path]: {
                        [method]: parsedSpec.paths[path][method],
                    },
                },
                servers: parsedSpec.servers,
            };

            return yamlStringify(endpointSpec);
        } catch {
            return specification;
        }
    }, [selectedEndpoint, specification]);

    const endpointYaml = useMemo(() => extractEndpointSpec(), [extractEndpointSpec]);

    const editorOptions = {
        automaticLayout: true,
        folding: true,
        lineNumbers: 'on' as const,
        minimap: {enabled: false},
        readOnly: true,
        scrollBeyondLastLine: false,
        wordWrap: 'on' as const,
    };

    const calculateEditorHeight = (content: string | null) => {
        if (!content) {
            return '200px';
        }

        const lineCount = content.split('\n').length;
        const lineHeight = 19;
        const minHeight = 200;
        const maxHeight = 500;
        const calculatedHeight = Math.min(Math.max(lineCount * lineHeight, minHeight), maxHeight);

        return `${calculatedHeight}px`;
    };

    if (!selectedEndpoint) {
        return null;
    }

    return (
        <Sheet onOpenChange={(open) => !open && closePanel()} open={isOpen}>
            <SheetContent className="flex w-full flex-col gap-0 p-0 sm:max-w-xl" side="right">
                <SheetHeader className="flex flex-row items-center justify-between gap-1 space-y-0 border-b p-4">
                    <div className="flex flex-col gap-2">
                        <SheetTitle className="flex items-center gap-3">
                            <Badge
                                className={twMerge(
                                    'w-20',
                                    getMethodBadgeColor(selectedEndpoint.httpMethod ?? undefined)
                                )}
                                label={selectedEndpoint.httpMethod || 'GET'}
                                styleType="outline-outline"
                                weight="semibold"
                            />

                            <span>{selectedEndpoint.name}</span>
                        </SheetTitle>

                        <p className="text-sm text-muted-foreground">{selectedEndpoint.path}</p>
                    </div>

                    <SheetCloseButton />
                </SheetHeader>

                <div className="flex-1 overflow-y-auto p-4">
                    <div className="flex flex-col gap-4">
                        <div>
                            <h3 className="text-sm font-medium">API Connector</h3>

                            <p className="text-sm text-muted-foreground">{apiConnectorName}</p>
                        </div>

                        {selectedEndpoint.description && (
                            <div>
                                <h3 className="text-sm font-medium">Description</h3>

                                <p className="text-sm text-muted-foreground">{selectedEndpoint.description}</p>
                            </div>
                        )}

                        {endpointYaml && (
                            <div>
                                <h3 className="mb-2 text-sm font-medium">OpenAPI Specification</h3>

                                <div
                                    className="overflow-hidden rounded-md border"
                                    style={{height: calculateEditorHeight(endpointYaml)}}
                                >
                                    <MonacoEditorWrapper
                                        defaultLanguage="yaml"
                                        onChange={() => {}}
                                        onMount={(editor) => {
                                            editorRef.current = editor;
                                        }}
                                        options={editorOptions}
                                        value={endpointYaml}
                                    />
                                </div>
                            </div>
                        )}

                        {!endpointYaml && specification && (
                            <div>
                                <h3 className="mb-2 text-sm font-medium">Full Specification</h3>

                                <div
                                    className="overflow-hidden rounded-md border"
                                    style={{height: calculateEditorHeight(specification)}}
                                >
                                    <MonacoEditorWrapper
                                        defaultLanguage="yaml"
                                        onChange={() => {}}
                                        onMount={(editor) => {
                                            editorRef.current = editor;
                                        }}
                                        options={editorOptions}
                                        value={specification}
                                    />
                                </div>
                            </div>
                        )}

                        {!specification && (
                            <div className="rounded-md border border-dashed p-4 text-center">
                                <p className="text-sm text-muted-foreground">No specification available.</p>
                            </div>
                        )}
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default ApiConnectorEndpointDetailPanel;
