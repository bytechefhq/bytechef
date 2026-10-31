import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {VersionWorkflowChange, WorkflowChangeType} from '@/shared/middleware/graphql';
import {Suspense, lazy, useState} from 'react';

import WorkflowChangeCanvasTab, {SideType, WorkflowChangeCanvasToolbar} from './WorkflowChangeCanvasTab';
import WorkflowDefinitionChangesList from './WorkflowDefinitionChangesList';

const MonacoDiffEditorWrapper = lazy(() => import('@/shared/components/MonacoDiffEditorWrapper'));
const MonacoEditorWrapper = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const DEFINITION_EDITOR_OPTIONS = {
    minimap: {enabled: false},
    readOnly: true,
};

interface WorkflowVersionChangesTabsProps {
    baseVersionLabel?: string;
    change: VersionWorkflowChange;
    versionLabel: string;
}

const WorkflowVersionChangesTabs = ({baseVersionLabel, change, versionLabel}: WorkflowVersionChangesTabsProps) => {
    const isModified = change.changeType === WorkflowChangeType.Modified;

    const [activeTab, setActiveTab] = useState(isModified ? 'changes' : 'canvas');
    const [selectedSide, setSelectedSide] = useState<SideType>('new');

    return (
        <Tabs className="gap-4" onValueChange={setActiveTab} value={activeTab}>
            <div className="flex items-center justify-between gap-4">
                <TabsList>
                    {isModified && <TabsTrigger value="changes">Changes</TabsTrigger>}

                    <TabsTrigger value="canvas">Canvas</TabsTrigger>

                    <TabsTrigger value="definition">Definition</TabsTrigger>
                </TabsList>

                {activeTab === 'canvas' && (
                    <WorkflowChangeCanvasToolbar
                        baseVersionLabel={baseVersionLabel}
                        change={change}
                        onSelectedSideChange={setSelectedSide}
                        selectedSide={selectedSide}
                        versionLabel={versionLabel}
                    />
                )}
            </div>

            {isModified && (
                <TabsContent value="changes">
                    <div className="h-[70vh] overflow-y-auto">
                        <WorkflowDefinitionChangesList changes={change.changes} />
                    </div>
                </TabsContent>
            )}

            <TabsContent value="canvas">
                <WorkflowChangeCanvasTab change={change} selectedSide={selectedSide} />
            </TabsContent>

            <TabsContent value="definition">
                <div className="h-[70vh]">
                    <Suspense fallback={<MonacoEditorLoader />}>
                        {isModified ? (
                            <MonacoDiffEditorWrapper
                                language="json"
                                modified={change.newDefinition ?? ''}
                                original={change.oldDefinition ?? ''}
                            />
                        ) : (
                            <MonacoEditorWrapper
                                defaultLanguage="json"
                                onChange={() => {}}
                                onMount={() => {}}
                                options={DEFINITION_EDITOR_OPTIONS}
                                value={change.newDefinition ?? change.oldDefinition ?? ''}
                            />
                        )}
                    </Suspense>
                </div>
            </TabsContent>
        </Tabs>
    );
};

interface WorkflowVersionChangesDialogProps {
    baseVersionLabel?: string;
    change?: VersionWorkflowChange;
    isError?: boolean;
    isLoading: boolean;
    onClose: () => void;
    open: boolean;
    versionLabel: string;
}

const WorkflowVersionChangesDialog = ({
    baseVersionLabel,
    change,
    isError,
    isLoading,
    onClose,
    open,
    versionLabel,
}: WorkflowVersionChangesDialogProps) => {
    const range = baseVersionLabel ? `${baseVersionLabel} → ${versionLabel}` : versionLabel;

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent className="sm:w-[min(1400px,calc(100vw-4rem))]" size="xl">
                <DialogMain>
                    <DialogHeader
                        description={range}
                        title={change ? change.label || change.workflowUuid : 'Workflow changes'}
                    />

                    <DialogBody>
                        {isError ? (
                            <p className="py-4 text-sm text-content-destructive">
                                Couldn&apos;t load the workflow changes.
                            </p>
                        ) : isLoading || !change ? (
                            <div className="flex justify-center py-4">
                                <LoadingIcon />
                            </div>
                        ) : (
                            <WorkflowVersionChangesTabs
                                baseVersionLabel={baseVersionLabel}
                                change={change}
                                key={change.workflowUuid}
                                versionLabel={versionLabel}
                            />
                        )}
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowVersionChangesDialog;
