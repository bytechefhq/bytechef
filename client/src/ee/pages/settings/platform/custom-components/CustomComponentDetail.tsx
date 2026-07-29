import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import PageLoader from '@/components/PageLoader';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    CustomComponent,
    CustomComponentActionDefinition,
    CustomComponentLanguage,
    CustomComponentTriggerDefinition,
    useCustomComponentDefinitionQuery,
    useCustomComponentQuery,
    useCustomComponentSourceQuery,
    useUpdateCustomComponentSourceMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon, ZapIcon} from 'lucide-react';
import {Suspense, lazy, useRef, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

const MonacoEditorWrapper = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const MONACO_LANGUAGE_BY_CUSTOM_COMPONENT_LANGUAGE: Record<CustomComponentLanguage, string> = {
    [CustomComponentLanguage.Java]: 'java',
    [CustomComponentLanguage.Javascript]: 'javascript',
    [CustomComponentLanguage.Python]: 'python',
    [CustomComponentLanguage.Ruby]: 'ruby',
};

interface CustomComponentDetailHeaderProps {
    customComponent: CustomComponent | undefined;
    isSaveDisabled: boolean;
    isSaving: boolean;
    onBack: () => void;
    onSave: () => void;
    showSaveButton: boolean;
}

const CustomComponentDetailHeader = ({
    customComponent,
    isSaveDisabled,
    isSaving,
    onBack,
    onSave,
    showSaveButton,
}: CustomComponentDetailHeaderProps) => (
    <Header
        centerTitle
        position="main"
        right={
            showSaveButton && (
                <Button disabled={isSaveDisabled} label={isSaving ? 'Saving...' : 'Save'} onClick={onSave} size="sm" />
            )
        }
        title={
            <div className="flex items-center gap-2">
                <Button
                    aria-label="Back"
                    icon={<ArrowLeftIcon className="size-5" />}
                    onClick={onBack}
                    size="icon"
                    variant="ghost"
                />

                <span>{customComponent?.title || customComponent?.name || 'Custom Component'}</span>

                {customComponent?.language && (
                    <Badge label={customComponent.language} styleType="secondary-filled" weight="semibold" />
                )}
            </div>
        }
    />
);

interface CustomComponentMetadataProps {
    customComponent: CustomComponent;
}

const CustomComponentMetadata = ({customComponent}: CustomComponentMetadataProps) => (
    <div className="border-b border-b-border/50 px-6 py-4">
        <div className="flex items-center gap-3">
            <span className="text-sm text-content-neutral-secondary">{customComponent.name}</span>

            {customComponent.componentVersion && (
                <Badge label={`v${customComponent.componentVersion}`} styleType="secondary-filled" weight="semibold" />
            )}

            <Badge
                label={customComponent.enabled ? 'Enabled' : 'Disabled'}
                styleType={customComponent.enabled ? 'success-filled' : 'secondary-filled'}
                weight="semibold"
            />
        </div>

        {customComponent.description && <p className="mt-2 text-sm text-gray-600">{customComponent.description}</p>}
    </div>
);

interface CustomComponentDefinitionListProps {
    items: Array<CustomComponentActionDefinition | CustomComponentTriggerDefinition>;
    title: string;
}

const CustomComponentDefinitionList = ({items, title}: CustomComponentDefinitionListProps) => (
    <div>
        <h4 className="mb-2 flex items-center gap-1 text-sm font-semibold text-gray-700">
            <ZapIcon className="size-4" />

            <span>
                {title} ({items.length})
            </span>
        </h4>

        {items.length > 0 ? (
            <ul className="space-y-2">
                {items.map((item) => (
                    <li className="rounded bg-gray-50 p-2" key={item.name}>
                        <div className="text-sm font-medium">{item.title || item.name}</div>

                        <div className="text-xs text-content-neutral-secondary">{item.name}</div>

                        {item.description && <div className="mt-1 text-xs text-gray-600">{item.description}</div>}
                    </li>
                ))}
            </ul>
        ) : (
            <p className="text-sm text-content-neutral-secondary">No {title.toLowerCase()} defined</p>
        )}
    </div>
);

interface CustomComponentJavaDefinitionProps {
    actions: Array<CustomComponentActionDefinition>;
    customComponent: CustomComponent;
    triggers: Array<CustomComponentTriggerDefinition>;
}

const CustomComponentJavaDefinition = ({actions, customComponent, triggers}: CustomComponentJavaDefinitionProps) => (
    <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
        <CustomComponentMetadata customComponent={customComponent} />

        <div className="grid grid-cols-2 gap-6 px-6 py-4">
            <CustomComponentDefinitionList items={actions} title="Actions" />

            <CustomComponentDefinitionList items={triggers} title="Triggers" />
        </div>
    </div>
);

interface CustomComponentSourceEditorProps {
    monacoLanguage: string;
    onChange: (value: string | undefined) => void;
    value: string;
}

const CustomComponentSourceEditor = ({monacoLanguage, onChange, value}: CustomComponentSourceEditorProps) => (
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        <div className="relative min-h-0 flex-1">
            <div className="absolute inset-0">
                <Suspense
                    fallback={
                        <div className="flex items-center justify-center p-8">
                            <LoadingIcon />
                        </div>
                    }
                >
                    <MonacoEditorWrapper
                        defaultLanguage={monacoLanguage}
                        onChange={onChange}
                        onMount={() => {}}
                        options={{
                            automaticLayout: true,
                            folding: true,
                            lineNumbers: 'on',
                            minimap: {enabled: false},
                            scrollBeyondLastLine: false,
                            wordWrap: 'on',
                        }}
                        value={value}
                    />
                </Suspense>
            </div>
        </div>
    </div>
);

const CustomComponentDetail = () => {
    const {id = ''} = useParams<{id: string}>();

    const [isSourceDirty, setIsSourceDirty] = useState(false);

    const latestSourceRef = useRef('');

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {
        data: customComponentData,
        error: customComponentError,
        isLoading: customComponentLoading,
    } = useCustomComponentQuery({id}, {enabled: !!id});

    const customComponent = customComponentData?.customComponent ?? undefined;
    const language = customComponent?.language ?? undefined;
    const isJavaComponent = language === CustomComponentLanguage.Java;
    const isEditableLanguage = !!language && !isJavaComponent;

    const {
        data: sourceData,
        error: sourceError,
        isLoading: sourceLoading,
    } = useCustomComponentSourceQuery({id}, {enabled: !!id && isEditableLanguage});

    const {
        data: definitionData,
        error: definitionError,
        isLoading: definitionLoading,
    } = useCustomComponentDefinitionQuery({id}, {enabled: !!id && isJavaComponent});

    const updateCustomComponentSourceMutation = useUpdateCustomComponentSourceMutation({
        onSuccess: () => {
            setIsSourceDirty(false);

            queryClient.invalidateQueries({queryKey: ['customComponentSource', {id}]});
        },
    });

    const sourceValue = sourceData?.customComponentSource ?? '';
    const actions = definitionData?.customComponentDefinition?.actions ?? [];
    const triggers = definitionData?.customComponentDefinition?.triggers ?? [];

    const handleBack = () => navigate(-1);

    const handleSave = () => {
        updateCustomComponentSourceMutation.mutate({content: latestSourceRef.current, id});
    };

    const handleSourceChange = (value: string | undefined) => {
        setIsSourceDirty(true);
        latestSourceRef.current = value ?? '';
    };

    return (
        <LayoutContainer
            header={
                <CustomComponentDetailHeader
                    customComponent={customComponent}
                    isSaveDisabled={!isSourceDirty || updateCustomComponentSourceMutation.isPending}
                    isSaving={updateCustomComponentSourceMutation.isPending}
                    onBack={handleBack}
                    onSave={handleSave}
                    showSaveButton={isEditableLanguage}
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader
                errors={[customComponentError, isJavaComponent ? definitionError : sourceError]}
                loading={customComponentLoading || (isJavaComponent ? definitionLoading : sourceLoading)}
            >
                {customComponent &&
                    (isJavaComponent ? (
                        <CustomComponentJavaDefinition
                            actions={actions}
                            customComponent={customComponent}
                            triggers={triggers}
                        />
                    ) : (
                        language && (
                            <CustomComponentSourceEditor
                                monacoLanguage={MONACO_LANGUAGE_BY_CUSTOM_COMPONENT_LANGUAGE[language]}
                                onChange={handleSourceChange}
                                value={isSourceDirty ? latestSourceRef.current : sourceValue}
                            />
                        )
                    ))}
            </PageLoader>
        </LayoutContainer>
    );
};

export default CustomComponentDetail;
