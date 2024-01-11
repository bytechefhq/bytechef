import {Switch} from '@/components/ui/switch';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import InlineSVG from 'react-inlinesvg';

interface UnifiedAPIIntegrationListItemProps {
    componentDefinition: ComponentDefinitionBasicModel;
}

const UnifiedAPIIntegrationListItem = ({componentDefinition}: UnifiedAPIIntegrationListItemProps) => {
    return (
        <div className="flex w-full items-center justify-between rounded-md px-2 py-5 hover:bg-gray-50">
            <div className="flex-1">
                <div className="flex items-center justify-between">
                    <div className="relative flex items-center gap-2">
                        <div className="flex items-center gap-2">
                            {componentDefinition?.icon && (
                                <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />
                            )}

                            <span className="text-base font-semibold text-gray-900">{componentDefinition?.title}</span>
                        </div>

                        {/*{componentDefinition.category && (*/}

                        {/*    <span className="text-xs uppercase text-gray-700">{componentDefinition.category.name}</span>*/}

                        {/*)}*/}
                    </div>
                </div>

                <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                    <div className="flex items-center"></div>
                </div>
            </div>

            <div className="flex items-center justify-end gap-x-6">
                <div className="flex flex-col items-end gap-y-4">
                    <Switch />
                </div>
            </div>
        </div>
    );
};

export default UnifiedAPIIntegrationListItem;
