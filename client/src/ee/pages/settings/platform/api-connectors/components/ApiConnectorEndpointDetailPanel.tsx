import Badge from '@/components/Badge/Badge';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import {twMerge} from 'tailwind-merge';

import {getHttpMethodBadgeColor} from '../utils/httpMethod-utils';
import useApiConnectorEndpointDetailPanel from './hooks/useApiConnectorEndpointDetailPanel';

const ApiConnectorEndpointDetailPanel = () => {
    const {
        apiConnectorName,
        calculateEditorHeight,
        closePanel,
        editorOptions,
        endpointYaml,
        isOpen,
        selectedEndpoint,
        specification,
    } = useApiConnectorEndpointDetailPanel();

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
                                className={twMerge('w-20', getHttpMethodBadgeColor(selectedEndpoint.httpMethod))}
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
                                        onMount={() => {}}
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
                                        onMount={() => {}}
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
