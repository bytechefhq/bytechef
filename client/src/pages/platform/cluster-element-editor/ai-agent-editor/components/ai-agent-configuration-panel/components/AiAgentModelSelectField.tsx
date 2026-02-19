import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowNodesPopoverMenu from '@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu';
import {BrainIcon, ComponentIcon, SettingsIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import useAiAgentModelSelectField from './hooks/useAiAgentModelSelectField';

export default function AiAgentModelSelectField() {
    const {handleConfigureModel, isConnectionMissing, model, rootWorkflowNodeName} = useAiAgentModelSelectField();

    return (
        <div>
            <h2 className="mb-2 flex items-center font-normal">Model:</h2>

            {rootWorkflowNodeName && (
                <WorkflowNodesPopoverMenu
                    clusterElementType="model"
                    hideActionComponents
                    hideTaskDispatchers
                    hideTriggerComponents
                    sourceNodeId={rootWorkflowNodeName}
                >
                    <Button
                        className={twMerge(
                            'w-full justify-start gap-2 font-normal',
                            isConnectionMissing && 'border-red-500'
                        )}
                        size="sm"
                        variant="outline"
                    >
                        {model ? (
                            <div className="flex w-full items-center justify-between">
                                <div className="flex items-center gap-1">
                                    {model.icon ? (
                                        <InlineSVG
                                            className="size-4 flex-none text-gray-700"
                                            loader={<ComponentIcon className="size-4 flex-none text-gray-700" />}
                                            src={model.icon}
                                        />
                                    ) : (
                                        <BrainIcon className="size-4 flex-none text-gray-700" />
                                    )}

                                    <span className="truncate">{model.title}</span>
                                </div>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span
                                            className="flex-none rounded p-0.5 hover:bg-muted"
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                handleConfigureModel(model);
                                            }}
                                            role="button"
                                        >
                                            <SettingsIcon className="size-4" />
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>Configure model</TooltipContent>
                                </Tooltip>
                            </div>
                        ) : (
                            <>
                                <BrainIcon className="size-4 flex-none text-muted-foreground" />

                                <span className="text-muted-foreground">Select a model...</span>
                            </>
                        )}
                    </Button>
                </WorkflowNodesPopoverMenu>
            )}
        </div>
    );
}
