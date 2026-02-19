import {ComboBoxItemType} from '@/components/ComboBox';
import {Button} from '@/components/ui/button';
import ComponentRow from '@/pages/automation/template/components/ComponentRow';
import TemplateLayoutContainer from '@/pages/automation/template/components/TemplateLayoutContainer';
import ProjectsComboBox from '@/pages/automation/template/workflow-template/components/ProjectsComboBox';
import WorkflowPreviewSvg from '@/pages/automation/template/workflow-template/components/WorkflowPreviewSvg';
import {useImportWorkflowTemplateMutation, useWorkflowTemplateQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const WorkflowTemplate = ({
    fromInternalFlow,
    sharedWorkflow = false,
}: {
    fromInternalFlow?: boolean;
    sharedWorkflow?: boolean;
}) => {
    const {id, projectId} = useParams();

    const [connectedComponents, setConnectedComponents] = useState<string[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>(projectId);

    const navigate = useNavigate();

    const {data: {workflowTemplate} = {}} = useWorkflowTemplateQuery({
        id: id!,
        sharedWorkflow,
    });

    const importWorkflowTemplateMutation = useImportWorkflowTemplateMutation({
        onSuccess: ({importWorkflowTemplate}) => {
            navigate(`/automation/projects/${selectedProjectId}/project-workflows/${importWorkflowTemplate}`);
        },
    });

    const handleClick = () => {
        if (selectedProjectId) {
            importWorkflowTemplateMutation.mutate({
                projectId: selectedProjectId,
                sharedWorkflow,
                workflowUuid: id!,
            });
        }
    };

    const handleOnChange = (item?: ComboBoxItemType) => {
        setSelectedProjectId(item?.value);
    };

    return (
        <TemplateLayoutContainer fromInternalFlow={fromInternalFlow}>
            <div className="flex w-7/12 flex-col space-y-5 p-5">
                <div>
                    <h1 className="mb-1 text-xl font-semibold text-primary">{workflowTemplate?.workflow?.label}</h1>

                    <p className="text-sm leading-relaxed text-muted-foreground">
                        {workflowTemplate?.description || 'No description available.'}
                    </p>
                </div>

                <div className="flex flex-col space-y-2">
                    <div>Choose Project:</div>

                    <ProjectsComboBox
                        onChange={handleOnChange}
                        value={selectedProjectId ? +selectedProjectId : undefined}
                    />
                </div>

                <div className="relative flex-1 space-y-4">
                    <span>This template contains the following components:</span>

                    <div className="absolute bottom-0 top-5 w-full space-y-3 overflow-y-auto">
                        {workflowTemplate?.components?.map((componentDefinition) => (
                            <ComponentRow
                                componentDefinition={componentDefinition!}
                                connectedComponents={connectedComponents}
                                key={componentDefinition!.name}
                                setConnectedComponents={setConnectedComponents}
                            />
                        ))}
                    </div>
                </div>

                <div className="flex justify-start">
                    <Button disabled={!selectedProjectId} onClick={handleClick}>
                        Import now
                    </Button>
                </div>
            </div>

            <div className="flex w-5/12 flex-col bg-muted">
                <div className="flex flex-1 items-center justify-center">
                    <WorkflowPreviewSvg className="h-auto max-w-full opacity-90" />
                </div>
            </div>
        </TemplateLayoutContainer>
    );
};

export default WorkflowTemplate;
