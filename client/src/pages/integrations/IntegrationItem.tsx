import {useState} from 'react';
import {Dropdown, DropDownMenuItem} from '../../components/DropDown/Dropdown';
import {CategoryModel, TagModel} from '../../data-access/integration';
import {useIntegrationTagsMutation} from '../../mutations/integrations.mutations';
import {IntegrationKeys} from '../../queries/integrations';
import {useQueryClient} from '@tanstack/react-query';
import {Content, Root, Trigger} from '@radix-ui/react-hover-card';
import cx from 'classnames';
import CreatableSelect from '../../components/CreatableSelect/CreatableSelect';
import Button from 'components/Button/Button';
import {PlusIcon, XMarkIcon} from '@heroicons/react/24/outline';
import {ChevronDownIcon} from '@radix-ui/react-icons';

const menuItems: DropDownMenuItem[] = [
    {
        label: 'Edit',
    },
    {
        label: 'Duplicate',
    },
    {
        label: 'New Workflow',
    },
    {
        separator: true,
    },
    {
        label: 'Delete',
    },
];

interface CategoryProps {
    category: CategoryModel;
}

interface IntegrationItemProps {
    category?: CategoryModel;
    description?: string;
    id?: number;
    lastDatePublished?: Date;
    name: string;
    published: boolean;
    remainingTags?: TagModel[];
    tags?: TagModel[];
    version?: number;
    workflowIds?: string[];
}

interface DateProps {
    lastPublishDate?: Date;
    published?: boolean;
}

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

interface WorkflowsProps {
    workflowIds?: string[];
}

const Category = ({category}: CategoryProps) => {
    return (
        <span className="text-xs uppercase text-gray-700">{category.name}</span>
    );
};

const Date = ({published, lastPublishDate}: DateProps) => {
    return (
        <span className="mr-4 w-[76px] text-center text-sm text-gray-500">
            {lastPublishDate &&
                published &&
                lastPublishDate.toLocaleDateString()}
            {!published && '-'}
        </span>
    );
};

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
            <Workflows workflowIds={workflowIds} />

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

            {category && <Category category={category} />}
        </div>
    );
};

const IntegrationItem = ({
    category,
    description,
    id,
    lastDatePublished,
    name,
    published,
    tags,
    version,
    workflowIds,
    remainingTags,
}: IntegrationItemProps) => {
    const queryClient = useQueryClient();

    const mutation = useIntegrationTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(IntegrationKeys.integrations);
            queryClient.invalidateQueries(IntegrationKeys.integrationTags);
        },
    });

    const handleOnAddTag = (newTag: TagModel) => {
        const newTags = (tags && [...tags]) || [];

        newTags.push(newTag);

        mutation.mutate({
            id: id || 0,
            putIntegrationTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

    const handleOnDeleteTag = (deletedTag: TagModel) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];

        mutation.mutate({
            id: id || 0,
            putIntegrationTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

    return (
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
                <Status published={published} version={version} />

                <Date
                    lastPublishDate={lastDatePublished}
                    published={published}
                />

                <Dropdown id={id} menuItems={menuItems} />
            </div>
        </div>
    );
};

const Name = ({name, description}: NameProps) => {
    return (
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
};

const Status = ({published, version}: StatusProps) => {
    const label = published ? 'Published' : 'Not Published';

    return (
        <span
            className={cx(
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

const Tag = ({tag, onDeleteTag}: TagProps) => {
    return (
        <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-700 hover:bg-gray-200">
            {tag.name}

            <XMarkIcon
                className="ml-1.5 h-3 w-3 rounded-full hover:bg-gray-300"
                onClick={() => onDeleteTag(tag)}
            />
        </span>
    );
};

const TagList = ({
    tags,
    remainingTags,
    onAddTag,
    onDeleteTag,
}: TagListProps) => {
    const [showAllTags, setShowAllTags] = useState(false);
    const [isNewTagWindowVisible, setIsNewTagWindowVisible] = useState(false);

    return (
        <div className="mr-4 flex items-center space-x-2">
            {tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id} tag={tag} onDeleteTag={onDeleteTag} />
            ))}

            {isNewTagWindowVisible ? (
                <CreatableSelect
                    className="w-40"
                    name="newTag"
                    isMulti={false}
                    options={remainingTags!.map((tag: TagModel) => ({
                        label: `${tag.name
                            .charAt(0)
                            .toUpperCase()}${tag.name.slice(1)}`,
                        value: tag.name.toLowerCase().replace(/\W/g, ''),
                        ...tag,
                    }))}
                    onCreateOption={(inputValue: string) => {
                        onAddTag({
                            name: inputValue,
                        });
                        setIsNewTagWindowVisible(false);
                    }}
                    onChange={(selectedOption) => {
                        remainingTags &&
                            onAddTag(
                                remainingTags.filter(
                                    (tag) =>
                                        tag.id != null &&
                                        tag.id === selectedOption!.id
                                )[0]
                            );
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
        </div>
    );
};

const Workflows = ({workflowIds}: WorkflowsProps) => {
    return (
        <div className="mr-4 text-xs font-semibold text-gray-700">
            {workflowIds && workflowIds.length}{' '}
            {workflowIds && workflowIds.length === 1 ? 'workflow' : 'workflows'}
        </div>
    );
};

export default IntegrationItem;
