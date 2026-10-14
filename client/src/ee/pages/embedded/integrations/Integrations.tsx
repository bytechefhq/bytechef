import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationsFilterTitle from '@/ee/pages/embedded/integrations/components/IntegrationsFilterTitle';
import NewIntegrationCodeWorkflowDialog from '@/ee/pages/embedded/integrations/components/NewIntegrationCodeWorkflowDialog';
import UnifiedApiLeftSidebarNav from '@/ee/pages/embedded/integrations/components/UnifiedApiLeftSidebarNav';
import IntegrationList from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationList';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationCategoriesQuery} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys, useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import ImportCodeWorkflowDialog from '@/pages/platform/code-workflow/components/ImportCodeWorkflowDialog';
import CategoryTagLeftSidebarNav from '@/shared/layout/CategoryTagLeftSidebarNav';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, CodeIcon, FileUpIcon, PlusIcon, SquareIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

export enum Type {
    Category,
    Tag,
    UnifiedAPI,
}

const Integrations = () => {
    const [newlyCreatedIntegrationId, setNewlyCreatedIntegrationId] = useState<number | undefined>();
    const [showImportCodeWorkflowDialog, setShowImportCodeWorkflowDialog] = useState(false);
    const [showIntegrationDialog, setShowIntegrationDialog] = useState(false);
    const [showNewCodeWorkflowDialog, setShowNewCodeWorkflowDialog] = useState(false);

    const [searchParams] = useSearchParams();

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');
    const unifiedApiCategory = searchParams.get('unifiedApiCategory') ?? undefined;

    const ff_743 = useFeatureFlagsStore()('ff-743');

    const filterData: {id: number | string | undefined; type: Type} = {
        id: categoryId ? parseInt(categoryId) : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Category,
    };

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const {
        data: integrations,
        error: integrationsError,
        isLoading: integrationsLoading,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    return (
        <LayoutContainer
            header={
                integrations &&
                integrations?.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            integrations &&
                            integrations.length > 0 && (
                                <ButtonGroup>
                                    <Button
                                        aria-label="Create Integration"
                                        label="New Integration"
                                        onClick={() => setShowIntegrationDialog(true)}
                                    />

                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button>
                                                <ChevronDownIcon />
                                            </Button>
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem onClick={() => setShowIntegrationDialog(true)}>
                                                <PlusIcon className="mr-2 size-4" />
                                                New Integration
                                            </DropdownMenuItem>

                                            <DropdownMenuItem onClick={() => setShowNewCodeWorkflowDialog(true)}>
                                                <CodeIcon className="mr-2 size-4" />
                                                New Code Integration
                                            </DropdownMenuItem>

                                            <DropdownMenuItem onClick={() => setShowImportCodeWorkflowDialog(true)}>
                                                <FileUpIcon className="mr-2 size-4" />
                                                Import Code Integration
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </ButtonGroup>
                            )
                        }
                        title={<IntegrationsFilterTitle categories={categories} filterData={filterData} tags={tags} />}
                    />
                )
            }
            leftSidebarBody={
                <CategoryTagLeftSidebarNav
                    categories={categories}
                    categoriesIsLoading={categoriesLoading}
                    currentCategoryId={categoryId ? parseInt(categoryId) : undefined}
                    currentTagId={tagId ? parseInt(tagId) : undefined}
                    extraGroups={ff_743 && <UnifiedApiLeftSidebarNav currentUnifiedApiCategory={unifiedApiCategory} />}
                    otherFilterActive={!!unifiedApiCategory}
                    tags={tags}
                    tagsClassName={ff_743 ? '' : 'mb-0'}
                    tagsEmptyMessage="No tags."
                    tagsIsLoading={tagsLoading}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Integrations" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[categoriesError, integrationsError, tagsError]}
                loading={categoriesLoading || integrationsLoading || tagsLoading}
            >
                {integrations && integrations?.length > 0 && tags ? (
                    <IntegrationList
                        componentDefinitions={componentDefinitions}
                        integrations={integrations}
                        newlyCreatedIntegrationId={newlyCreatedIntegrationId}
                        tags={tags}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                    />
                ) : (
                    <EmptyList
                        button={
                            <ButtonGroup className="mx-auto">
                                <Button
                                    aria-label="Create Integration"
                                    label="Create Integration"
                                    onClick={() => setShowIntegrationDialog(true)}
                                />

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button>
                                            <ChevronDownIcon />
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <DropdownMenuItem onClick={() => setShowIntegrationDialog(true)}>
                                            <PlusIcon className="mr-2 size-4" />
                                            New Integration
                                        </DropdownMenuItem>

                                        <DropdownMenuItem onClick={() => setShowNewCodeWorkflowDialog(true)}>
                                            <CodeIcon className="mr-2 size-4" />
                                            New Code Integration
                                        </DropdownMenuItem>

                                        <DropdownMenuItem onClick={() => setShowImportCodeWorkflowDialog(true)}>
                                            <FileUpIcon className="mr-2 size-4" />
                                            Import Code Integration
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </ButtonGroup>
                        }
                        icon={<SquareIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new integrations."
                        title="No Integrations"
                    />
                )}
            </PageLoader>

            {showIntegrationDialog && (
                <IntegrationDialog
                    integration={undefined}
                    onClose={(integration) => {
                        setShowIntegrationDialog(false);

                        if (integration) {
                            navigate(
                                `/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}`
                            );
                        }
                    }}
                    onSuccess={(integrationId) => integrationId && setNewlyCreatedIntegrationId(integrationId)}
                />
            )}

            {showImportCodeWorkflowDialog && (
                <ImportCodeWorkflowDialog
                    deployUrl="/api/embedded/internal/integrations/deploy"
                    description="Upload a built code integration. If a code integration for the same component exists, it is redeployed."
                    fileFieldName="integrationFile"
                    onClose={() => setShowImportCodeWorkflowDialog(false)}
                    onImported={() => queryClient.invalidateQueries({queryKey: IntegrationKeys.integrations})}
                    title="Import Code Integration"
                />
            )}

            {showNewCodeWorkflowDialog && (
                <NewIntegrationCodeWorkflowDialog onClose={() => setShowNewCodeWorkflowDialog(false)} />
            )}
        </LayoutContainer>
    );
};

export default Integrations;
