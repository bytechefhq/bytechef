import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ComponentRow from '@/pages/automation/template/components/ComponentRow';
import TemplateLayoutContainer from '@/pages/automation/template/components/TemplateLayoutContainer';
import WorkflowPreviewSvg from '@/pages/automation/template/workflow-template/components/WorkflowPreviewSvg';
import {useImportProjectTemplateMutation, useProjectTemplateQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const ProjectTemplate = ({
    fromInternalFlow,
    sharedProject = false,
}: {
    fromInternalFlow?: boolean;
    sharedProject?: boolean;
}) => {
    const [connectedComponents, setConnectedComponents] = useState<string[]>([]);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();
    const {id} = useParams();

    const {data: {projectTemplate} = {}} = useProjectTemplateQuery({
        id: id!,
        sharedProject,
    });

    const importProjectTemplateMutation = useImportProjectTemplateMutation({
        onSuccess: () => {
            navigate('/automation/projects');
        },
    });

    const handleClick = () => {
        importProjectTemplateMutation.mutate({
            id: id!,
            sharedProject,
            workspaceId: currentWorkspaceId?.toString() ?? '',
        });
    };

    return (
        <TemplateLayoutContainer fromInternalFlow={fromInternalFlow}>
            <div className="flex w-7/12 flex-col space-y-5 p-6">
                <div>
                    <h1 className="mb-1 text-xl font-semibold text-primary">{projectTemplate?.project?.name}</h1>

                    <p className="text-sm leading-relaxed text-muted-foreground">
                        {projectTemplate?.description || 'No description available.'}
                    </p>
                </div>

                <div className="relative flex-1 space-y-4">
                    <span>This template contains the following components:</span>

                    {projectTemplate && (
                        <div className="absolute bottom-0 top-5 w-full space-y-3 overflow-y-auto">
                            {projectTemplate.workflows.map((workflow) => {
                                const componentDefinitions = projectTemplate?.components?.find(
                                    (component) => component!.key === workflow!.id
                                )!.value;

                                return (
                                    <div className="flex-1 space-y-4" key={workflow?.id}>
                                        <h3 className="font-semibold">{workflow?.label}</h3>

                                        <div className="space-y-3">
                                            {componentDefinitions.map((componentDefinition) => (
                                                <ComponentRow
                                                    componentDefinition={componentDefinition!}
                                                    connectedComponents={connectedComponents}
                                                    key={componentDefinition!.name}
                                                    setConnectedComponents={setConnectedComponents}
                                                />
                                            ))}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                <div className="flex justify-start">
                    <Button onClick={handleClick}>Import now</Button>
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

export default ProjectTemplate;
