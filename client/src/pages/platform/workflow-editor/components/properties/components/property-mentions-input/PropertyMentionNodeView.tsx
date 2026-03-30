import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {
    buildValidDataPillReferenceSet,
    isDataPillReferenceValid,
} from '@/pages/platform/workflow-editor/utils/dataPillReferenceValidation';
import {buildUnavailableDataPillHoverTitle} from '@/pages/platform/workflow-editor/utils/unavailableDatapillHoverTitle';
import {NodeViewWrapper} from '@tiptap/react';
import {XIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {getDataPillIconSource} from './getDataPillIconSource';
import {
    PROPERTY_MENTION_CHIP_CLASS,
    PROPERTY_MENTION_LABEL_CLASS,
    PROPERTY_MENTION_ROOT_CLASS,
} from './propertyMentionDom';

import type {NodeViewProps} from '@tiptap/react';

function PropertyMentionNodeView({HTMLAttributes, deleteNode, editor, node}: NodeViewProps) {
    const {class: htmlAttributeClass, ...restHtmlAttributes} = HTMLAttributes as {
        class?: string;
        [key: string]: unknown;
    };
    const {componentDefinitions, dataPills, taskDispatcherDefinitions, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            componentDefinitions: state.componentDefinitions,
            dataPills: state.dataPills,
            taskDispatcherDefinitions: state.taskDispatcherDefinitions,
            workflow: state.workflow,
        }))
    );

    const validReferenceSet = useMemo(() => buildValidDataPillReferenceSet(dataPills), [dataPills]);

    const mentionId = node.attrs.id as string | null | undefined;
    const dataPillLabel = `${node.attrs.label ?? node.attrs.id ?? ''}`;

    const isUnavailable =
        mentionId != null && validReferenceSet.size > 0 && !isDataPillReferenceValid(mentionId, validReferenceSet);

    const controlType = editor.storage.MentionStorage.controlType as string | undefined;

    const iconSource = useMemo(
        () =>
            getDataPillIconSource({
                componentDefinitions,
                mentionDisplay: node.attrs.label ?? node.attrs.id ?? '',
                taskDispatcherDefinitions,
                workflow,
            }),
        [componentDefinitions, node.attrs.id, node.attrs.label, taskDispatcherDefinitions, workflow]
    );

    const unavailableTooltipText = useMemo(() => {
        if (!isUnavailable || mentionId == null) {
            return undefined;
        }

        return buildUnavailableDataPillHoverTitle({
            componentDefinitions,
            mentionId,
            taskDispatcherDefinitions,
            workflow,
        });
    }, [componentDefinitions, isUnavailable, mentionId, taskDispatcherDefinitions, workflow]);

    const dataPillChipClassName = twMerge(
        PROPERTY_MENTION_CHIP_CLASS,
        'relative inline-flex items-center gap-0.5 rounded-full bg-muted px-2',
        controlType !== 'RICH_TEXT' && controlType !== 'TEXT_AREA' && controlType !== 'FORMULA_MODE' && 'text-sm',
        isUnavailable && 'rounded-l-full rounded-r-none bg-transparent pl-2 pr-1'
    );

    const dataPillChip = (
        <span className={dataPillChipClassName}>
            <img alt="Dynamic Value Icon" className="absolute size-4" draggable={false} src={iconSource} />

            <span className={twMerge(PROPERTY_MENTION_LABEL_CLASS, 'ml-5')}>{dataPillLabel}</span>
        </span>
    );

    return (
        <NodeViewWrapper
            {...restHtmlAttributes}
            as="span"
            className={twMerge(
                htmlAttributeClass,
                PROPERTY_MENTION_ROOT_CLASS,
                'not-prose inline-flex max-w-full items-stretch',
                isUnavailable && 'property-mention--unavailable'
            )}
        >
            {unavailableTooltipText != null ? (
                <Tooltip>
                    <TooltipTrigger asChild>{dataPillChip}</TooltipTrigger>

                    <TooltipContent className="max-w-sm whitespace-pre-wrap text-left">
                        {unavailableTooltipText}
                    </TooltipContent>
                </Tooltip>
            ) : (
                dataPillChip
            )}

            {isUnavailable && (
                <Button
                    aria-label="Remove unavailable data pill"
                    className="size-5 hover:bg-transparent [&_svg]:size-3"
                    icon={<XIcon aria-hidden />}
                    onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();

                        deleteNode();
                    }}
                    size="iconXs"
                    variant="destructiveGhost"
                />
            )}
        </NodeViewWrapper>
    );
}

PropertyMentionNodeView.displayName = 'PropertyMentionNodeView';

export default PropertyMentionNodeView;
