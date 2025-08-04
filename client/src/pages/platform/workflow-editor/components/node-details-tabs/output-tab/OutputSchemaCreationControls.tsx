import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';

interface OutputSchemaCreationControlsProps {
    handleTestOperationClick: () => void;
    setShowUploadDialog: (show: boolean) => void;
    saveWorkflowNodeTestOutputMutationPending: boolean;
    trigger?: boolean;
    uploadSampleOutputRequestMutationPending: boolean;
    variablePropertiesDefined?: boolean;
}

const OutputSchemaCreationControls = ({
    handleTestOperationClick,
    saveWorkflowNodeTestOutputMutationPending,
    setShowUploadDialog,
    trigger = false,
    uploadSampleOutputRequestMutationPending,
    variablePropertiesDefined = false,
}: OutputSchemaCreationControlsProps) => (
    <div className="flex size-full items-center justify-center">
        <div className="flex flex-col items-center gap-8">
            <div className="flex w-full flex-col gap-1">
                <div className="self-center">Define Output Schema</div>

                <p className="text-sm text-muted-foreground">
                    {!variablePropertiesDefined
                        ? 'Define the expected output schema with one of the methods'
                        : 'Define the expected output schema by uploading sample data'}
                </p>
            </div>

            <div className="flex flex-col gap-4">
                {!variablePropertiesDefined && (
                    <div className="flex w-full flex-col gap-3">
                        <Button
                            disabled={saveWorkflowNodeTestOutputMutationPending}
                            onClick={handleTestOperationClick}
                            type="button"
                        >
                            {`Test ${trigger ? 'Trigger' : 'Action'}`}
                        </Button>

                        <span className="text-center">or</span>
                    </div>
                )}

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
            </div>
        </div>
    </div>
);

export default OutputSchemaCreationControls;
