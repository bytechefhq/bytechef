import {Type} from '@/pages/automation/project-deployments/ProjectDeployments';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {ModeType, useMcpServerTagsQuery} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

const McpServersLeftSidebarNav = () => {
    const [searchParams] = useSearchParams();

    const environment = searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined;
    const projectId = searchParams.get('projectId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: projectId ? projectId : tagId ? tagId : undefined,
        type: tagId ? Type.Tag : Type.Project,
    };

    const {data, isLoading: tagsIsLoading} = useMcpServerTagsQuery({type: ModeType.Automation});

    if (!data || !data?.mcpServerTags) {
        return <></>;
    }

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        {[
                            {label: 'All Environments'},
                            {label: 'Development', value: 1},
                            {label: 'Staging', value: 2},
                            {label: 'Production', value: 3},
                        ]?.map((item) => (
                            <LeftSidebarNavItem
                                item={{
                                    current: environment === item.value,
                                    id: item.value,
                                    name: item.label,
                                }}
                                key={item.value ?? ''}
                                toLink={`?environment=${item.value ?? ''}${filterData.id ? `&${filterData.type === Type.Project ? 'projectId' : 'tagId'}=${filterData.id}` : ''}`}
                            />
                        ))}
                    </>
                }
                title="Environments"
            />

            <LeftSidebarNav
                body={
                    <>
                        {!tagsIsLoading &&
                            (data.mcpServerTags.length ? (
                                data.mcpServerTags.map((item) => (
                                    <LeftSidebarNavItem
                                        icon={<TagIcon className="mr-1 size-4" />}
                                        item={{
                                            current: filterData?.id === item!.id && filterData.type === Type.Tag,
                                            id: item!.id,
                                            name: item!.name,
                                        }}
                                        key={item!.id}
                                        toLink={`?tagId=${item!.id}&environment=${environment ?? ''}`}
                                    />
                                ))
                            ) : (
                                <span className="px-3 text-xs">No defined tags.</span>
                            ))}
                    </>
                }
                title="Tags"
            />
        </>
    );
};

export default McpServersLeftSidebarNav;
