import {
    CommandDialog,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from '@/components/ui/command';
import {SearchAssetType, useAutomationSearchQuery} from '@/shared/middleware/graphql';
import {keepPreviousData} from '@tanstack/react-query';
import {
    FileTextIcon,
    FolderIcon,
    Layers3Icon,
    LayoutTemplateIcon,
    Link2Icon,
    RouteIcon,
    Table2Icon,
    VectorSquareIcon,
    ZapIcon,
} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useDebouncedCallback} from 'use-debounce';

interface GlobalSearchDialogProps {
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const GlobalSearchDialog = ({onOpenChange, open}: GlobalSearchDialogProps) => {
    const [query, setQuery] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');
    const navigate = useNavigate();

    const updateDebouncedQuery = useDebouncedCallback((value: string) => {
        setDebouncedQuery(value);
    }, 300);

    const {data} = useAutomationSearchQuery(
        {limit: 20, query: debouncedQuery},
        {
            enabled: debouncedQuery.length >= 2,
            placeholderData: keepPreviousData,
        }
    );

    const groupedResults = useMemo(() => {
        const results = data?.automationSearch ?? [];

        return {
            apiCollections: results.filter((result) => result.type === SearchAssetType.ApiCollection),
            apiEndpoints: results.filter((result) => result.type === SearchAssetType.ApiEndpoint),
            connections: results.filter((result) => result.type === SearchAssetType.Connection),
            dataTables: results.filter((result) => result.type === SearchAssetType.DataTable),
            deployments: results.filter((result) => result.type === SearchAssetType.Deployment),
            knowledgeBaseDocuments: results.filter((result) => result.type === SearchAssetType.KnowledgeBaseDocument),
            knowledgeBases: results.filter((result) => result.type === SearchAssetType.KnowledgeBase),
            projects: results.filter((result) => result.type === SearchAssetType.Project),
            workflows: results.filter((result) => result.type === SearchAssetType.Workflow),
        };
    }, [data?.automationSearch]);

    const handleSelect = useCallback(
        (path: string) => {
            navigate(path);
            onOpenChange(false);
            setQuery('');
        },
        [navigate, onOpenChange]
    );

    useEffect(() => {
        if (!open) {
            setQuery('');
            setDebouncedQuery('');
        }
    }, [open]);

    const hasResults = (data?.automationSearch?.length ?? 0) > 0;

    return (
        <CommandDialog onOpenChange={onOpenChange} open={open} shouldFilter={false}>
            <CommandInput
                className="my-2"
                onValueChange={(value) => {
                    setQuery(value);
                    updateDebouncedQuery(value);
                }}
                placeholder="Search projects, workflows, connections..."
                value={query}
            />

            <CommandList>
                {debouncedQuery.length < 2 ? (
                    <CommandEmpty>Type at least 2 characters to search...</CommandEmpty>
                ) : !hasResults ? (
                    <CommandEmpty>No results found.</CommandEmpty>
                ) : (
                    <>
                        {groupedResults.projects.length > 0 && (
                            <CommandGroup heading="Projects">
                                {groupedResults.projects.map((project) => (
                                    <CommandItem
                                        key={`project-${project.id}`}
                                        onSelect={() => handleSelect(`/automation/projects/${project.id}`)}
                                    >
                                        <FolderIcon className="mr-2 size-4" />

                                        <span>{project.name}</span>

                                        {project.description && (
                                            <span className="ml-2 text-xs text-muted-foreground">
                                                {project.description}
                                            </span>
                                        )}
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.workflows.length > 0 && (
                            <CommandGroup heading="Workflows">
                                {groupedResults.workflows.map((workflow) => (
                                    <CommandItem
                                        key={`workflow-${workflow.id}`}
                                        onSelect={() =>
                                            handleSelect(
                                                `/automation/projects/${'projectId' in workflow ? workflow.projectId : ''}/workflows/${workflow.id}`
                                            )
                                        }
                                    >
                                        <ZapIcon className="mr-2 size-4" />

                                        <span>{'label' in workflow ? workflow.label : workflow.name}</span>

                                        {workflow.description && (
                                            <span className="ml-2 text-xs text-muted-foreground">
                                                {workflow.description}
                                            </span>
                                        )}
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.deployments.length > 0 && (
                            <CommandGroup heading="Deployments">
                                {groupedResults.deployments.map((deployment) => (
                                    <CommandItem
                                        key={`deployment-${deployment.id}`}
                                        onSelect={() => handleSelect(`/automation/deployments/${deployment.id}`)}
                                    >
                                        <Layers3Icon className="mr-2 size-4" />

                                        <span>
                                            {'projectName' in deployment ? deployment.projectName : deployment.name}
                                        </span>
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.apiCollections.length > 0 && (
                            <CommandGroup heading="API Collections">
                                {groupedResults.apiCollections.map((collection) => (
                                    <CommandItem
                                        key={`api-collection-${collection.id}`}
                                        onSelect={() => handleSelect(`/automation/api-collections/${collection.id}`)}
                                    >
                                        <LayoutTemplateIcon className="mr-2 size-4" />

                                        <span>{collection.name}</span>

                                        {collection.description && (
                                            <span className="ml-2 text-xs text-muted-foreground">
                                                {collection.description}
                                            </span>
                                        )}
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.apiEndpoints.length > 0 && (
                            <CommandGroup heading="API Endpoints">
                                {groupedResults.apiEndpoints.map((endpoint) => (
                                    <CommandItem
                                        key={`api-endpoint-${endpoint.id}`}
                                        onSelect={() =>
                                            handleSelect(
                                                `/automation/api-collections/${'collectionId' in endpoint ? endpoint.collectionId : ''}/endpoints/${endpoint.id}`
                                            )
                                        }
                                    >
                                        <RouteIcon className="mr-2 size-4" />

                                        <span>{endpoint.name}</span>

                                        {'path' in endpoint && endpoint.path && (
                                            <span className="ml-2 font-mono text-xs text-muted-foreground">
                                                {endpoint.path}
                                            </span>
                                        )}
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.dataTables.length > 0 && (
                            <CommandGroup heading="Data Tables">
                                {groupedResults.dataTables.map((table) => (
                                    <CommandItem
                                        key={`data-table-${table.id}`}
                                        onSelect={() => handleSelect(`/automation/data-tables/${table.id}`)}
                                    >
                                        <Table2Icon className="mr-2 size-4" />

                                        <span>{table.name}</span>
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.knowledgeBases.length > 0 && (
                            <CommandGroup heading="Knowledge Bases">
                                {groupedResults.knowledgeBases.map((knowledgeBase) => (
                                    <CommandItem
                                        key={`kb-${knowledgeBase.id}`}
                                        onSelect={() => handleSelect(`/automation/knowledge-bases/${knowledgeBase.id}`)}
                                    >
                                        <VectorSquareIcon className="mr-2 size-4" />

                                        <span>{knowledgeBase.name}</span>

                                        {knowledgeBase.description && (
                                            <span className="ml-2 text-xs text-muted-foreground">
                                                {knowledgeBase.description}
                                            </span>
                                        )}
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.knowledgeBaseDocuments.length > 0 && (
                            <CommandGroup heading="KB Documents">
                                {groupedResults.knowledgeBaseDocuments.map((document) => (
                                    <CommandItem
                                        key={`kb-doc-${document.id}`}
                                        onSelect={() =>
                                            handleSelect(
                                                `/automation/knowledge-bases/${'knowledgeBaseId' in document ? document.knowledgeBaseId : ''}/documents/${document.id}`
                                            )
                                        }
                                    >
                                        <FileTextIcon className="mr-2 size-4" />

                                        <span>{document.name}</span>
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}

                        {groupedResults.connections.length > 0 && (
                            <CommandGroup heading="Connections">
                                {groupedResults.connections.map((connection) => (
                                    <CommandItem
                                        key={`connection-${connection.id}`}
                                        onSelect={() => handleSelect(`/automation/connections/${connection.id}`)}
                                    >
                                        <Link2Icon className="mr-2 size-4" />

                                        <span>{connection.name}</span>
                                    </CommandItem>
                                ))}
                            </CommandGroup>
                        )}
                    </>
                )}
            </CommandList>
        </CommandDialog>
    );
};

export default GlobalSearchDialog;
