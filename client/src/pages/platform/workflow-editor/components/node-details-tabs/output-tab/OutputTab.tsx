/// <reference types="vite-plugin-svgr/client" />

import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Input} from '@/components/ui/input';
import ClusterElementTestPropertiesPopover from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/ClusterElementTestPropertiesPopover';
import OutputSchemaCreationControls from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputSchemaCreationControls';
import OutputSchemaDisplay from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputSchemaDisplay';
import OutputTabSampleDataDialog from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog';
import DialogLoader from '@/shared/components/DialogLoader';
import {TriggerType} from '@/shared/middleware/platform/configuration';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {AlertCircleIcon, ClipboardIcon} from 'lucide-react';
import {Suspense} from 'react';
import {twMerge} from 'tailwind-merge';

import useOutputTab from './hooks/useOutputTab';

interface OutputTabProps {
    clusterElementType?: string;
    connectionMissing: boolean;
    currentNode: NodeDataType;
    currentOperationProperties?: PropertyAllType[];
    outputDefined?: boolean;
    outputFunctionDefined?: boolean;
    parentWorkflowNodeName?: string;
    variablePropertiesDefined?: boolean;
    workflowId: string;
}

const OutputTab = ({
    clusterElementType,
    connectionMissing,
    currentNode,
    currentOperationProperties,
    outputDefined,
    outputFunctionDefined,
    parentWorkflowNodeName,
    variablePropertiesDefined = false,
    workflowId,
}: OutputTabProps) => {
    const {
        copiedValue,
        copyToClipboard,
        handleClusterElementTestSubmit,
        handlePredefinedOutputSchemaClick,
        handleSampleDataDialogUpload,
        handleTestCancelClick,
        handleTestOperationClick,
        hasClusterElementProperties,
        outputSchema,
        placeholder,
        sampleOutput,
        saveClusterElementTestOutputMutationPending,
        saveWorkflowNodeTestOutputMutation,
        saveWorkflowNodeTestOutputMutationPending,
        setShowUploadDialog,
        setTestPropertiesPopoverOpen,
        showUploadDialog,
        testPropertiesPopoverOpen,
        testing,
        uploadSampleOutputRequestMutationPending,
        webhookTestCancelEnabled,
        webhookTestUrl,
        workflowNodeOutputIsFetching,
    } = useOutputTab({
        clusterElementType,
        currentNode,
        currentOperationProperties,
        parentWorkflowNodeName,
        workflowId,
    });

    function renderClusterElementTestButton() {
        if (!hasClusterElementProperties) {
            return undefined;
        }

        return (
            <ClusterElementTestPropertiesPopover
                currentNode={currentNode}
                onOpenChange={setTestPropertiesPopoverOpen}
                onSubmit={handleClusterElementTestSubmit}
                open={testPropertiesPopoverOpen}
                properties={currentOperationProperties!}
            >
                <Button
                    disabled={connectionMissing || saveClusterElementTestOutputMutationPending}
                    label="Test Action"
                    variant="outline"
                />
            </ClusterElementTestPropertiesPopover>
        );
    }

    if (!testing && workflowNodeOutputIsFetching) {
        return <></>;
    }

    if (!testing && outputFunctionDefined && !outputSchema) {
        return <div className="p-4 text-sm text-muted-foreground">No output schema to show.</div>;
    }

    return (
        <div className="h-full p-4">
            {!testing && (
                <div className="h-full">
                    {outputSchema && (
                        <OutputSchemaDisplay
                            connectionMissing={connectionMissing}
                            copiedValue={copiedValue}
                            copyToClipboard={copyToClipboard}
                            currentNode={currentNode}
                            handlePredefinedOutputSchemaClick={handlePredefinedOutputSchemaClick}
                            handleTestOperationClick={handleTestOperationClick}
                            outputSchema={outputSchema}
                            sampleOutput={sampleOutput}
                            saveWorkflowNodeTestOutputMutation={saveWorkflowNodeTestOutputMutation}
                            setShowUploadDialog={setShowUploadDialog}
                            testActionButton={renderClusterElementTestButton()}
                            variablePropertiesDefined={variablePropertiesDefined}
                        />
                    )}

                    {!outputSchema && (
                        <OutputSchemaCreationControls
                            handleTestOperationClick={handleTestOperationClick}
                            outputDefined={outputDefined}
                            saveWorkflowNodeTestOutputMutationPending={saveWorkflowNodeTestOutputMutationPending}
                            setShowUploadDialog={setShowUploadDialog}
                            showUploadSampleOutputButton={outputDefined}
                            testActionButton={renderClusterElementTestButton()}
                            trigger={currentNode.trigger}
                            uploadSampleOutputRequestMutationPending={uploadSampleOutputRequestMutationPending}
                            variablePropertiesDefined={variablePropertiesDefined}
                        />
                    )}
                </div>
            )}

            {testing && (
                <Suspense
                    fallback={
                        <div className="flex items-center justify-center p-4">
                            <LoadingIcon />

                            <span>Loading Testing UI...</span>
                        </div>
                    }
                >
                    <div className="flex size-full flex-col items-center justify-center gap-6">
                        <div
                            className={twMerge(
                                'flex',
                                currentNode.triggerType !== TriggerType.Polling &&
                                    currentNode.triggerType !== TriggerType.Hybrid &&
                                    'w-full justify-between pl-2',
                                (currentNode.triggerType === TriggerType.Polling ||
                                    currentNode.triggerType === TriggerType.Hybrid) &&
                                    'flex-col gap-2'
                            )}
                        >
                            <div
                                className={twMerge(
                                    'mt-1 flex items-center justify-center',
                                    !currentNode.trigger && 'w-full'
                                )}
                            >
                                <LoadingIcon />

                                <span className="text-lg">{`Testing ${currentNode.trigger ? 'Trigger' : 'Action'}`}</span>
                            </div>

                            {currentNode.trigger &&
                                currentNode.triggerType !== TriggerType.Polling &&
                                currentNode.triggerType !== TriggerType.Hybrid && (
                                    <Button
                                        className="flex items-center gap-2"
                                        disabled={!webhookTestCancelEnabled}
                                        label="Cancel"
                                        onClick={handleTestCancelClick}
                                        size="sm"
                                        variant="outline"
                                    />
                                )}
                        </div>

                        {currentNode.trigger &&
                            currentNode.triggerType !== TriggerType.Polling &&
                            currentNode.triggerType !== TriggerType.Hybrid && (
                                <Alert>
                                    <AlertCircleIcon className="size-4" />

                                    <AlertTitle>Action Required</AlertTitle>

                                    <AlertDescription className="flex flex-col gap-1">
                                        {currentNode.triggerType === TriggerType.StaticWebhook ? (
                                            <>
                                                <div>Please call the following webhook test URL</div>
                                                <div className="relative">
                                                    <Input className="pr-8" disabled value={webhookTestUrl} />

                                                    <ClipboardIcon
                                                        aria-hidden="true"
                                                        className="absolute right-0 top-2.5 mx-2 size-4 cursor-pointer text-gray-400 hover:text-gray-800 group-hover:visible"
                                                        onClick={() => copyToClipboard(webhookTestUrl!)}
                                                    />
                                                </div>
                                                <div>by sending sample data</div>{' '}
                                            </>
                                        ) : (
                                            <div>
                                                Please go to your service and make an action that will activate this
                                                trigger
                                            </div>
                                        )}
                                    </AlertDescription>
                                </Alert>
                            )}
                    </div>
                </Suspense>
            )}

            {showUploadDialog && (
                <Suspense fallback={<DialogLoader />}>
                    <OutputTabSampleDataDialog
                        onClose={() => setShowUploadDialog(false)}
                        onUpload={handleSampleDataDialogUpload}
                        open={showUploadDialog}
                        placeholder={placeholder || sampleOutput}
                    />
                </Suspense>
            )}
        </div>
    );
};

export default OutputTab;
