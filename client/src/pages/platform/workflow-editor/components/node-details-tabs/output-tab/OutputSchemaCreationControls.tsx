import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import ClusterElementTestButton from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/ClusterElementTestButton';
import {NodeDataType, PropertyAllType} from '@/shared/types';

interface OutputSchemaCreationControlsProps {
    clusterElementType?: string;
    connectionMissing?: boolean;
    currentNode?: NodeDataType;
    currentOperationProperties?: PropertyAllType[];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleClusterElementTestSubmit?: (inputParameters: Record<string, any>, onSuccess?: () => void) => void;
    handleTestOperationClick: () => void;
    outputDefined?: boolean;
    saveClusterElementTestOutputMutationPending?: boolean;
    saveWorkflowNodeTestOutputMutationPending: boolean;
    setShowUploadDialog: (show: boolean) => void;
    showClusterElementTestButton?: boolean;
    showUploadSampleOutputButton?: boolean;
    trigger?: boolean;
    uploadSampleOutputRequestMutationPending: boolean;
    variablePropertiesDefined?: boolean;
}

const OutputSchemaCreationControls = ({
    clusterElementType,
    connectionMissing,
    currentNode,
    currentOperationProperties,
    handleClusterElementTestSubmit,
    handleTestOperationClick,
    outputDefined,
    saveClusterElementTestOutputMutationPending,
    saveWorkflowNodeTestOutputMutationPending,
    setShowUploadDialog,
    showClusterElementTestButton,
    showUploadSampleOutputButton = false,
    trigger = false,
    uploadSampleOutputRequestMutationPending,
    variablePropertiesDefined = false,
}: OutputSchemaCreationControlsProps) => {
    const operationLabel = clusterElementType === 'tools' ? 'Tool' : trigger ? 'Trigger' : 'Action';

    return (
        <div className="flex size-full items-center justify-center">
            <div className="flex flex-col items-center gap-8">
                {clusterElementType === 'tools' ? (
                    <div className="flex w-full flex-col gap-1">
                        <div className="self-center">Explore Output Schema</div>

                        <p className="text-sm text-muted-foreground">
                            Explore the expected output schema by testing tool
                        </p>
                    </div>
                ) : outputDefined || variablePropertiesDefined ? (
                    <div className="flex w-full flex-col gap-1">
                        <div className="self-center">Define Output Schema</div>

                        <p className="text-sm text-muted-foreground">
                            {!variablePropertiesDefined
                                ? 'Define the expected output schema with one of the methods'
                                : 'Define the expected output schema by uploading sample data'}
                        </p>
                    </div>
                ) : (
                    <div className="flex w-full flex-col gap-1">
                        <div className="self-center">Output Test</div>

                        <p className="text-sm text-muted-foreground">
                            {`Test the ${operationLabel.toLowerCase()} to see the expected behaviour.`}
                        </p>
                    </div>
                )}

                <div className="flex flex-col gap-4">
                    {!variablePropertiesDefined && (
                        <div className="flex w-full flex-col gap-3">
                            {showClusterElementTestButton &&
                            currentNode &&
                            currentOperationProperties &&
                            handleClusterElementTestSubmit ? (
                                <ClusterElementTestButton
                                    clusterElementType={clusterElementType}
                                    connectionMissing={!!connectionMissing}
                                    currentNode={currentNode}
                                    onSubmit={handleClusterElementTestSubmit}
                                    properties={currentOperationProperties}
                                    saving={!!saveClusterElementTestOutputMutationPending}
                                />
                            ) : (
                                <Button
                                    disabled={saveWorkflowNodeTestOutputMutationPending}
                                    label={`Test ${operationLabel}`}
                                    onClick={handleTestOperationClick}
                                    type="button"
                                />
                            )}

                            {clusterElementType !== 'tools' && showUploadSampleOutputButton && (
                                <span className="text-center">or</span>
                            )}
                        </div>
                    )}

                    {clusterElementType !== 'tools' && showUploadSampleOutputButton && (
                        <Button
                            disabled={uploadSampleOutputRequestMutationPending}
                            onClick={() => setShowUploadDialog(true)}
                            type="button"
                        >
                            {uploadSampleOutputRequestMutationPending && (
                                <>
                                    <LoadingIcon />

                                    <span>Uploading...</span>
                                </>
                            )}

                            {!uploadSampleOutputRequestMutationPending && <span>Upload Sample Output Data</span>}
                        </Button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default OutputSchemaCreationControls;
