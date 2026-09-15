import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import ClusterElementTestButton from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/ClusterElementTestButton';
import OutputTabTestBlockedNotice from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabTestBlockedNotice';
import OutputTabTestButton from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabTestButton';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {NodeDataType, PropertyAllType} from '@/shared/types';

interface OutputSchemaCreationControlsProps {
    clusterElementType?: string;
    connectionMissing?: boolean;
    currentNode?: NodeDataType;
    currentOperationProperties?: PropertyAllType[];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleClusterElementTestSubmit?: (inputParameters: Record<string, any>, onSuccess?: () => void) => void;
    handleTestOperationClick: () => void;
    missingRequiredProperties?: string[];
    onMissingRequiredPropertiesClick?: () => void;
    outputDefined?: boolean;
    saveClusterElementTestOutputMutationPending?: boolean;
    saveWorkflowNodeTestOutputMutationPending: boolean;
    setShowUploadDialog: (show: boolean) => void;
    showClusterElementTestButton?: boolean;
    showUploadSampleOutputButton?: boolean;
    testable?: boolean;
    trigger?: boolean;
    uploadSampleOutputRequestMutationPending: boolean;
}

const OutputSchemaCreationControls = ({
    clusterElementType,
    connectionMissing,
    currentNode,
    currentOperationProperties,
    handleClusterElementTestSubmit,
    handleTestOperationClick,
    missingRequiredProperties = [],
    onMissingRequiredPropertiesClick,
    outputDefined,
    saveClusterElementTestOutputMutationPending,
    saveWorkflowNodeTestOutputMutationPending,
    setShowUploadDialog,
    showClusterElementTestButton,
    showUploadSampleOutputButton = false,
    testable = true,
    trigger = false,
    uploadSampleOutputRequestMutationPending,
}: OutputSchemaCreationControlsProps) => {
    const readOnly = useWorkflowEditorReadOnly();

    const operationLabel = clusterElementType === 'tools' ? 'Tool' : trigger ? 'Trigger' : 'Action';

    const title =
        clusterElementType === 'tools'
            ? 'Explore Output Schema'
            : outputDefined
              ? 'Define Output Schema'
              : `${operationLabel} Test`;

    const description =
        clusterElementType === 'tools'
            ? 'Explore the expected output schema by testing the tool'
            : outputDefined
              ? testable
                  ? 'Define the expected output schema with one of the methods'
                  : 'Define the expected output schema by uploading sample output data'
              : `Test the ${operationLabel.toLowerCase()} to see the expected behavior as this ${operationLabel.toLowerCase()} does not define the output.`;

    return (
        <div className="flex w-full pt-4">
            <div className="flex w-full flex-col gap-6">
                <div className="flex flex-col gap-1">
                    <h3 className="text-sm font-medium">{title}</h3>

                    <p className="text-sm text-muted-foreground">{description}</p>
                </div>

                {!readOnly && (
                    <div className="flex w-full flex-col gap-4">
                        {testable && (
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
                                    <>
                                        <OutputTabTestBlockedNotice
                                            className="mb-5"
                                            connectionMissing={!!connectionMissing}
                                            missingRequiredProperties={missingRequiredProperties}
                                            onMissingRequiredPropertiesClick={onMissingRequiredPropertiesClick}
                                            operationLabel={operationLabel}
                                        />

                                        <OutputTabTestButton
                                            connectionMissing={!!connectionMissing}
                                            label={`Test ${operationLabel}`}
                                            missingRequiredProperties={missingRequiredProperties}
                                            onClick={handleTestOperationClick}
                                            pending={saveWorkflowNodeTestOutputMutationPending}
                                        />
                                    </>
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
                )}
            </div>
        </div>
    );
};

export default OutputSchemaCreationControls;
