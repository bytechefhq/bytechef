import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import PageLoader from '@/components/PageLoader';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    CustomComponent,
    CustomComponentActionDefinition,
    CustomComponentLanguage,
    CustomComponentStatus,
    CustomComponentTriggerDefinition,
    useCustomComponentDefinitionQuery,
    useCustomComponentQuery,
    useCustomComponentSourceQuery,
    usePublishCustomComponentMutation,
    useUpdateCustomComponentSourceMutation,
} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon, SparklesIcon, ZapIcon} from 'lucide-react';
import {Suspense, lazy, useEffect, useRef, useState} from 'react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';

const MonacoEditorWrapper = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const MONACO_LANGUAGE_BY_CUSTOM_COMPONENT_LANGUAGE: Record<CustomComponentLanguage, string> = {
    [CustomComponentLanguage.Java]: 'java',
    [CustomComponentLanguage.Javascript]: 'javascript',
    [CustomComponentLanguage.Python]: 'python',
    [CustomComponentLanguage.Ruby]: 'ruby',
};

interface CustomComponentDetailHeaderProps {
    customComponent: CustomComponent | undefined;
    isPublishDisabled: boolean;
    isPublishing: boolean;
    isSaveDisabled: boolean;
    isSaving: boolean;
    onAskCopilot?: () => void;
    onBack?: () => void;
    onPublish: () => void;
    onSave: () => void;
    showSaveButton: boolean;
    status?: CustomComponentStatus;
}

const CustomComponentDetailHeader = ({
    customComponent,
    isPublishDisabled,
    isPublishing,
    isSaveDisabled,
    isSaving,
    onAskCopilot,
    onBack,
    onPublish,
    onSave,
    showSaveButton,
    status,
}: CustomComponentDetailHeaderProps) => (
    <Header
        centerTitle
        position="main"
        right={
            showSaveButton && (
                <div className="flex items-center gap-2">
                    <Button
                        disabled={isPublishDisabled}
                        label={isPublishing ? 'Publishing...' : 'Publish'}
                        onClick={onPublish}
                        size="sm"
                        variant="outline"
                    />

                    <Button
                        disabled={isSaveDisabled}
                        label={isSaving ? 'Saving...' : 'Save'}
                        onClick={onSave}
                        size="sm"
                    />

                    {onAskCopilot && (
                        <Button
                            aria-label="Ask Copilot"
                            icon={<SparklesIcon />}
                            onClick={onAskCopilot}
                            size="iconSm"
                            variant="ghost"
                        />
                    )}
                </div>
            )
        }
        title={
            <div className="flex items-center gap-2">
                {onBack && (
                    <Button
                        aria-label="Back"
                        icon={<ArrowLeftIcon className="size-5" />}
                        onClick={onBack}
                        size="icon"
                        variant="ghost"
                    />
                )}

                <span>{customComponent?.title || customComponent?.name || 'Custom Component'}</span>

                {customComponent?.language && (
                    <Badge label={customComponent.language} styleType="secondary-filled" weight="semibold" />
                )}

                {status && (
                    <Badge
                        label={status === CustomComponentStatus.Draft ? 'Draft' : 'Published'}
                        styleType={status === CustomComponentStatus.Draft ? 'secondary-filled' : 'success-filled'}
                        weight="semibold"
                    />
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

        {customComponent.description && (
            <p className="mt-2 text-sm text-content-neutral-secondary">{customComponent.description}</p>
        )}
    </div>
);

interface CustomComponentDefinitionListProps {
    items: Array<CustomComponentActionDefinition | CustomComponentTriggerDefinition>;
    title: string;
}

const CustomComponentDefinitionList = ({items, title}: CustomComponentDefinitionListProps) => (
    <div>
        <h4 className="mb-2 flex items-center gap-1 text-sm font-semibold text-content-neutral-secondary">
            <ZapIcon className="size-4" />

            <span>
                {title} ({items.length})
            </span>
        </h4>

        {items.length > 0 ? (
            <ul className="space-y-2">
                {items.map((item) => (
                    <li className="rounded bg-surface-neutral-secondary p-2" key={item.name}>
                        <div className="text-sm font-medium">{item.title || item.name}</div>

                        <div className="text-xs text-content-neutral-secondary">{item.name}</div>

                        {item.description && (
                            <div className="mt-1 text-xs text-content-neutral-secondary">{item.description}</div>
                        )}
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

interface CustomComponentDetailProps {
    customComponentId?: string;
}

const CustomComponentDetail = ({customComponentId: customComponentIdProp}: CustomComponentDetailProps = {}) => {
    const params = useParams<{id: string}>();

    const id = customComponentIdProp ?? params.id ?? '';

    const [isSourceDirty, setIsSourceDirty] = useState(false);

    const latestSourceRef = useRef('');

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);
    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);

    const location = useLocation();
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
        onSuccess: (result) => {
            setIsSourceDirty(false);

            const updatedId = result.updateCustomComponentSource.id;

            if (updatedId && updatedId !== id) {
                queryClient.invalidateQueries({queryKey: ['customComponents']});

                // Route-driven mode (id from useParams) navigates to the new draft's URL. In
                // prop-driven mode (id from the customComponentId prop, e.g. the AI Hub resource
                // panel embedding) there is no owned route to rewrite into -- navigating would
                // send the host app to a broken path, so only invalidate.
                if (!customComponentIdProp) {
                    navigate(location.pathname.replace(/\/[^/]+$/, `/${updatedId}`));
                }
            } else {
                queryClient.invalidateQueries({queryKey: ['customComponent', {id}]});
                queryClient.invalidateQueries({queryKey: ['customComponentSource', {id}]});
            }
        },
    });

    const publishCustomComponentMutation = usePublishCustomComponentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['customComponent', {id}]});
            queryClient.invalidateQueries({queryKey: ['customComponents']});
        },
    });

    const sourceValue = sourceData?.customComponentSource ?? '';
    const actions = definitionData?.customComponentDefinition?.actions ?? [];
    const triggers = definitionData?.customComponentDefinition?.triggers ?? [];

    const isPublishDisabled =
        customComponent?.status !== CustomComponentStatus.Draft ||
        isSourceDirty ||
        publishCustomComponentMutation.isPending;

    const handleBack = customComponentIdProp ? undefined : () => navigate(-1);

    // Suppressed in the prop-driven embedding (AI Hub resource panel) -- that surface already lives inside a
    // copilot conversation.
    const handleAskCopilot =
        copilotEnabled && !customComponentIdProp
            ? () => {
                  setContext({
                      mode: MODE.ASK,
                      parameters: {customComponentId: id},
                      source: Source.CUSTOM_COMPONENT,
                  });

                  setCopilotPanelOpen(true);
              }
            : undefined;

    const handlePublish = () => {
        publishCustomComponentMutation.mutate({id});
    };

    const handleSave = () => {
        updateCustomComponentSourceMutation.mutate({content: latestSourceRef.current, id});
    };

    const handleSourceChange = (value: string | undefined) => {
        setIsSourceDirty(true);
        latestSourceRef.current = value ?? '';
    };

    useEffect(() => {
        // A Copilot BUILD turn can create or rewrite components server-side; refresh the detail and list
        // queries after each turn so the editor and badges reflect the change.
        return registerPostTurn(Source.CUSTOM_COMPONENT, () => {
            queryClient.invalidateQueries({queryKey: ['customComponent', {id}]});
            queryClient.invalidateQueries({queryKey: ['customComponentSource', {id}]});
            queryClient.invalidateQueries({queryKey: ['customComponents']});
        });
    }, [id, queryClient, registerPostTurn]);

    return (
        <LayoutContainer
            header={
                <CustomComponentDetailHeader
                    customComponent={customComponent}
                    isPublishDisabled={isPublishDisabled}
                    isPublishing={publishCustomComponentMutation.isPending}
                    isSaveDisabled={!isSourceDirty || updateCustomComponentSourceMutation.isPending}
                    isSaving={updateCustomComponentSourceMutation.isPending}
                    onAskCopilot={handleAskCopilot}
                    onBack={handleBack}
                    onPublish={handlePublish}
                    onSave={handleSave}
                    showSaveButton={isEditableLanguage}
                    status={customComponent?.status ?? undefined}
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
