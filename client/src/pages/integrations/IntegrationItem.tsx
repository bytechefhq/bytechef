import {useState} from 'react';
import {Dropdown, DropDownMenuItem} from '../../components/DropDown/Dropdown';
import {
    CategoryModel,
    IntegrationModel,
    TagModel,
} from '../../middleware/integration';
import {
    useIntegrationDeleteMutation,
    useIntegrationMutation,
    useIntegrationTagsMutation,
} from '../../mutations/integrations.mutations';
import {IntegrationKeys} from '../../queries/integrations';
import {useQueryClient} from '@tanstack/react-query';
import {Content, Root, Trigger} from '@radix-ui/react-hover-card';
import {twMerge} from 'tailwind-merge';
import CreatableSelect, {
    SelectOption,
} from '../../components/CreatableSelect/CreatableSelect';
import Button from 'components/Button/Button';
import {PlusIcon, XMarkIcon} from '@heroicons/react/24/outline';
import {ChevronDownIcon} from '@radix-ui/react-icons';
import IntegrationModal from './IntegrationModal';
import {useGetIntegrationQuery} from 'queries/integrations';
import {OnChangeValue} from 'react-select';

interface FooterProps {
    tags?: TagModel[];
    workflowIds?: string[];
    remainingTags?: TagModel[];
    onAddTag: (newTag: TagModel) => void;
    onDeleteTag: (deletedTag: TagModel) => void;
}

interface HeaderProps {
    category?: CategoryModel;
    description?: string;
    name: string;
}

interface NameProps {
    description: string;
    name: string;
}

interface StatusProps {
    published: boolean;
    version?: number;
}

interface TagProps {
    tag: TagModel;
    onDeleteTag: (deletedTag: TagModel) => void;
}

interface TagListProps {
    tags: TagModel[];
    onAddTag: (newTag: TagModel) => void;
    onDeleteTag: (deletedTag: TagModel) => void;
    remainingTags?: TagModel[];
}

const Footer = ({
    tags,
    workflowIds,
    remainingTags,
    onAddTag,
    onDeleteTag,
}: FooterProps) => {
    return (
        <div
            className="flex h-[38px] items-center"
            onClick={(event) => event.preventDefault()}
        >
            <div className="mr-4 text-xs font-semibold text-gray-700">
                {workflowIds?.length === 1
                    ? `${workflowIds?.length} workflow`
                    : `${workflowIds?.length} workflows`}
            </div>

            {tags && (
                <TagList
                    tags={tags}
                    remainingTags={remainingTags}
                    onAddTag={onAddTag}
                    onDeleteTag={onDeleteTag}
                />
            )}
        </div>
    );
};

const Header = ({
    category,
    description = 'Description not available',
    name,
}: HeaderProps) => {
    return (
        <div className="relative mb-2 flex items-center">
            <Name description={description} name={name} />

            {category && (
                <span className="text-xs uppercase text-gray-700">
                    {category.name}
                </span>
            )}
        </div>
    );
};

interface IntegrationItemProps {
    integrationNames: string[];
    name: string;
    published: boolean;
    category?: CategoryModel;
    description?: string;
    id?: number;
    lastDatePublished?: Date;
    remainingTags?: TagModel[];
    tags?: TagModel[];
    componentVersion?: number;
    workflowIds?: string[];
}

const IntegrationItem = ({
    category,
    description,
    id,
    lastDatePublished,
    name,
    published,
    tags,
    componentVersion,
    workflowIds,
    remainingTags,
    integrationNames,
}: IntegrationItemProps) => {
    const [showEditModal, setShowEditModal] = useState<boolean>(false);

    const dropdownItems: DropDownMenuItem[] = [
        {
            label: 'Edit',
            onClick: (id: number, event: React.MouseEvent) => {
                event.preventDefault();

                setShowEditModal(true);
            },
        },
        {
            label: 'Duplicate',
            onClick: (id: number, event: React.MouseEvent) => {
                event.preventDefault();

                duplicateIntegrationItem();
            },
        },
        {
            label: 'New Workflow',
        },
        {
            separator: true,
        },
        {
            label: 'Delete',
            onClick: (id: number, event: React.MouseEvent) => {
                event.preventDefault();

                if (
                    confirm('Are you sure you want to delete this integration?')
                ) {
                    handleOnConfirmDelete();
                }
            },
        },
    ];

    const queryClient = useQueryClient();

    const {data: currentIntegration} = useGetIntegrationQuery(id!);

    const duplication = useIntegrationMutation({
        onSuccess: (data) => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);

            queryClient.invalidateQueries(
                IntegrationKeys.integrationCategories
            );

            queryClient.invalidateQueries(IntegrationKeys.integrationTags);

            const integrationsData = queryClient.getQueryData<
                IntegrationModel[]
            >(IntegrationKeys.integrations);

            if (integrationsData) {
                queryClient.setQueryData<IntegrationModel[]>(
                    IntegrationKeys.integrations,
                    [...integrationsData, data]
                );
            }
        },
    });

    const duplicateIntegrationItem = () => {
        let copyNumber = 1;

        while (
            integrationNames.includes(
                `${currentIntegration?.name} (${copyNumber})`
            )
        ) {
            copyNumber++;
        }

        duplication.mutate({
            ...currentIntegration,
            id: undefined,
            name: `${currentIntegration?.name} (${copyNumber})`,
            version: undefined,
        } as IntegrationModel);
    };

    const deletion = useIntegrationDeleteMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);
        },
    });

    const handleOnConfirmDelete = () => {
        if (id) {
            deletion.mutate({
                id,
            });
        }
    };

    const tagsMutation = useIntegrationTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);
        },
    });

    const handleOnAddTag = (newTag: TagModel) => {
        const newTags = (tags && [...tags]) || [];

        newTags.push(newTag);

        tagsMutation.mutate({
            id: id || 0,
            putIntegrationTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

    const handleOnDeleteTag = (deletedTag: TagModel) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];

        tagsMutation.mutate({
            id: id || 0,
            putIntegrationTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

    return (
        <>
            <div className="flex items-center justify-between">
                <div>
                    <Header
                        category={category}
                        description={description}
                        name={name}
                    />

                    <Footer
                        tags={tags}
                        workflowIds={workflowIds}
                        remainingTags={remainingTags}
                        onAddTag={handleOnAddTag}
                        onDeleteTag={handleOnDeleteTag}
                    />
                </div>

                <div className="flex items-center">
                    <Status published={published} version={componentVersion} />

                    <span className="mr-4 w-[76px] text-center text-sm text-gray-500">
                        {lastDatePublished && published
                            ? lastDatePublished.toLocaleDateString()
                            : '-'}
                    </span>

                    <Dropdown id={id} menuItems={dropdownItems} />
                </div>
            </div>

            {showEditModal && (
                <IntegrationModal
                    id={id}
                    integrationItem={currentIntegration}
                    visible
                />
            )}
        </>
    );
};

const Name = ({name, description}: NameProps) => (
    <Root>
        <Trigger asChild>
            <span className="mr-2 text-base font-semibold text-gray-900">
                {name}
            </span>
        </Trigger>

        {description && (
            <Content
                align="center"
                className="max-w-md rounded-lg bg-white p-4 shadow-lg dark:bg-gray-800 md:w-full"
                sideOffset={4}
            >
                <div className="flex h-full w-full space-x-4">
                    <p className="mt-1 text-sm font-normal text-gray-700 dark:text-gray-400">
                        {description}
                    </p>
                </div>
            </Content>
        )}
    </Root>
);

const Status = ({published, version}: StatusProps) => {
    const label = published ? 'Published' : 'Not Published';

    return (
        <span
            className={twMerge(
                'mr-4 rounded px-2.5 py-0.5 text-sm font-medium',
                published ? 'bg-green-100' : 'bg-gray-100',
                published ? 'text-green-800' : 'text-gray-800',
                published ? 'dark:bg-green-200' : 'dark:bg-gray-200',
                published ? 'dark:text-green-900' : 'dark:text-gray-900'
            )}
        >
            {label} {published && `V${version}`}
        </span>
    );
};

const Tag = ({tag, onDeleteTag}: TagProps) => (
    <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-700 hover:bg-gray-200">
        {tag.name}

        <XMarkIcon
            className="ml-1.5 h-3 w-3 rounded-full hover:bg-gray-300"
            onClick={() => onDeleteTag(tag)}
        />
    </span>
);

const TagList = ({
    tags,
    remainingTags,
    onAddTag,
    onDeleteTag,
}: TagListProps) => {
    const [showAllTags, setShowAllTags] = useState(false);
    const [isNewTagWindowVisible, setIsNewTagWindowVisible] = useState(false);

    return (
        <div className="mr-4 flex items-center space-x-1">
            {tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id} tag={tag} onDeleteTag={onDeleteTag} />
            ))}

            {tags.length > 3 && (
                <div className="relative flex">
                    <Button
                        className="mr-2"
                        size="small"
                        displayType="unstyled"
                        icon={<ChevronDownIcon />}
                        onClick={() => setShowAllTags(!showAllTags)}
                    />

                    {showAllTags && (
                        <div
                            className="absolute left-0 top-full z-10 w-px space-y-2 border-0 bg-white shadow-lg"
                            style={{maxHeight: '32px'}}
                        >
                            {tags.slice(3).map((tag) => (
                                <Tag
                                    key={tag.id}
                                    tag={tag}
                                    onDeleteTag={onDeleteTag}
                                />
                            ))}
                        </div>
                    )}
                </div>
            )}

            {isNewTagWindowVisible ? (
                <CreatableSelect
                    className="w-40"
                    name="newTag"
                    options={remainingTags!.map((tag: TagModel) => ({
                        label: `${tag.name
                            .charAt(0)
                            .toUpperCase()}${tag.name.slice(1)}`,
                        value: tag.name.toLowerCase().replace(/\W/g, ''),
                        tag,
                    }))}
                    onCreateOption={(inputValue: string) => {
                        onAddTag({
                            name: inputValue,
                        });
                        setIsNewTagWindowVisible(false);
                    }}
                    onChange={(
                        selectedOption: OnChangeValue<SelectOption, false>
                    ) => {
                        if (selectedOption) {
                            onAddTag(selectedOption.tag);
                        }

                        setIsNewTagWindowVisible(false);
                    }}
                />
            ) : (
                <div
                    className="flex h-6 w-6 cursor-pointer items-center justify-center rounded bg-gray-100 hover:bg-gray-200"
                    onClick={(event) => {
                        event.preventDefault();

                        setIsNewTagWindowVisible(true);
                    }}
                >
                    <PlusIcon className="h-3 w-3 rounded-full hover:bg-gray-300" />
                </div>
            )}
        </div>
    );
};

export default IntegrationItem;
