import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EmptyList from '@/components/EmptyList';
import {ButtonGroup} from '@/components/ui/button-group';
import {Skeleton} from '@/components/ui/skeleton';
import IntegrationWorkflowListItem from '@/ee/pages/embedded/integrations/components/integration-workflow-list/IntegrationWorkflowListItem';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {useCreateIntegrationWorkflowMutation} from '@/ee/shared/mutations/embedded/workflows.mutations';
import {useGetIntegrationWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import useButtonGroupDropdownAlign from '@/shared/hooks/useButtonGroupDropdownAlign';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, UploadIcon, WorkflowIcon} from 'lucide-react';
import {ChangeEvent, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

const IntegrationWorkflowList = ({
    componentDefinitions,
    integration,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    integration: Integration;
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);

    const {captureIntegrationWorkflowCreated, captureIntegrationWorkflowImported} = useAnalytics();
    const {alignOffset, buttonGroupRef, dropdownMenuTriggerRef, handleOpenChange} = useButtonGroupDropdownAlign();

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const {data: workflows, isLoading: isIntegrationWorkflowsLoading} = useGetIntegrationWorkflowsQuery(
        integration.id!
    );

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (integrationWorkflowId) => {
            captureIntegrationWorkflowCreated();

            void navigate(`/embedded/integrations/${integration.id}/integration-workflows/${integrationWorkflowId}`);
        },
    });

    const importIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: () => {
            captureIntegrationWorkflowImported();

            void queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integrations,
            });

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast('Workflow is imported.');
        },
    });

    const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.files?.length) {
            importIntegrationWorkflowMutation.mutate({
                id: integration.id!,
                workflow: {
                    definition: await event.target.files[0].text(),
                },
            });
        }
    };

    return !componentDefinitions || !taskDispatcherDefinitions || isIntegrationWorkflowsLoading ? (
        <div className="space-y-3 p-3">
            <Skeleton className="h-5 w-40" />

            {[1, 2].map((value) => (
                <div className="flex items-center space-x-4" key={value}>
                    <Skeleton className="h-4 w-80" />

                    <div className="flex w-60 items-center space-x-1">
                        <Skeleton className="h-6 w-7 rounded-full" />

                        <Skeleton className="size-7 rounded-full" />

                        <Skeleton className="size-7 rounded-full" />
                    </div>

                    <Skeleton className="h-4 flex-1" />
                </div>
            ))}
        </div>
    ) : (
        <div>
            {workflows && workflows.length > 0 ? (
                <>
                    <ul className="divide-y divide-stroke-neutral-primary">
                        {workflows
                            .sort((a, b) => a.label!.localeCompare(b.label!))
                            .map((workflow) => {
                                const componentNames = [
                                    ...(workflow.workflowTriggerComponentNames ?? []),
                                    ...(workflow.workflowTaskComponentNames ?? []),
                                ];

                                componentNames?.map((definitionName) => {
                                    if (!workflowComponentDefinitions[definitionName]) {
                                        workflowComponentDefinitions[definitionName] = componentDefinitions?.find(
                                            (componentDefinition) => componentDefinition.name === definitionName
                                        );
                                    }

                                    if (!workflowTaskDispatcherDefinitions[definitionName]) {
                                        workflowTaskDispatcherDefinitions[definitionName] =
                                            taskDispatcherDefinitions?.find(
                                                (taskDispatcherDefinition) =>
                                                    taskDispatcherDefinition.name === definitionName
                                            );
                                    }
                                });

                                const filteredComponentNames = componentNames?.filter(
                                    (item, index) => componentNames?.indexOf(item) === index
                                );

                                return (
                                    <IntegrationWorkflowListItem
                                        filteredComponentNames={filteredComponentNames}
                                        integration={integration}
                                        key={workflow.id}
                                        workflow={workflow}
                                        workflowComponentDefinitions={workflowComponentDefinitions}
                                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                                    />
                                );
                            })}
                    </ul>
                </>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={
                            <ButtonGroup className="mx-auto" ref={buttonGroupRef}>
                                <Button onClick={() => setShowWorkflowDialog(true)}>Create Workflow</Button>

                                <DropdownMenu onOpenChange={handleOpenChange}>
                                    <DropdownMenuTrigger asChild>
                                        <Button
                                            aria-label="More Workflow Creation Actions"
                                            icon={<ChevronDownIcon />}
                                            ref={dropdownMenuTriggerRef}
                                        />
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="start" alignOffset={alignOffset}>
                                        <DropdownMenuItem
                                            icon={<UploadIcon />}
                                            label="Import Workflow"
                                            onClick={() => hiddenFileInputRef.current?.click()}
                                        />
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </ButtonGroup>
                        }
                        icon={<WorkflowIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new workflow."
                        title="No Workflows"
                    />
                </div>
            )}

            {showWorkflowDialog && (
                <WorkflowDialog
                    createWorkflowMutation={createIntegrationWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    parentId={integration.id}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input
                accept=".json,.yaml,.yml"
                className="hidden"
                onChange={handleFileChange}
                ref={hiddenFileInputRef}
                type="file"
            />
        </div>
    );
};

export default IntegrationWorkflowList;
