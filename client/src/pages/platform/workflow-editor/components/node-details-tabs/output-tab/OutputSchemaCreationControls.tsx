import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';

interface OutputSchemaCreationControlsProps {
    handleTestOperationClick: () => void;
    outputDefined?: boolean;
    setShowUploadDialog: (show: boolean) => void;
    saveWorkflowNodeTestOutputMutationPending: boolean;
    showUploadSampleOutputButton?: boolean;
    trigger?: boolean;
    uploadSampleOutputRequestMutationPending: boolean;
    variablePropertiesDefined?: boolean;
}

const OutputSchemaCreationControls = ({
    handleTestOperationClick,
    outputDefined,
    saveWorkflowNodeTestOutputMutationPending,
    setShowUploadDialog,
    showUploadSampleOutputButton = false,
    trigger = false,
    uploadSampleOutputRequestMutationPending,
    variablePropertiesDefined = false,
}: OutputSchemaCreationControlsProps) => (
    <div className="flex size-full items-center justify-center">
        <div className="flex flex-col items-center gap-8">
            {outputDefined || variablePropertiesDefined ? (
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
                        {`Test the ${trigger ? 'trigger' : 'action'} to see the expected behaviour.`}
                    </p>
                </div>
            )}

            <div className="flex flex-col gap-4">
                {!variablePropertiesDefined && (
                    <div className="flex w-full flex-col gap-3">
                        <Button
                            disabled={saveWorkflowNodeTestOutputMutationPending}
                            label={`Test ${trigger ? 'Trigger' : 'Action'}`}
                            onClick={handleTestOperationClick}
                            type="button"
                        />

                        {showUploadSampleOutputButton && <span className="text-center">or</span>}
                    </div>
                )}

                {showUploadSampleOutputButton && (
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

export default OutputSchemaCreationControls;
