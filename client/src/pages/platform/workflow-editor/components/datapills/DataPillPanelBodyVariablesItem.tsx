import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import DataPill from '@/pages/platform/workflow-editor/components/datapills/DataPill';
import useWorkflowVariables from '@/pages/platform/workflow-editor/hooks/useWorkflowVariables';
import {VARIABLES_NODE_NAME} from '@/pages/platform/workflow-editor/utils/getWorkflowInputAndVariableDataPills';
import {PropertyType} from '@/shared/middleware/platform/configuration';
import {AccordionContent, AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon, VariableIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

interface DataPillPanelBodyVariablesItemProps {
    dataPillFilterQuery: string;
}

const AUTOMATION_VARIABLES_SETTINGS_PATH = '/automation/settings/variables';
const EMBEDDED_VARIABLES_SETTINGS_PATH = '/embedded/settings/variables';

const DataPillPanelBodyVariablesItem = ({dataPillFilterQuery}: DataPillPanelBodyVariablesItemProps) => {
    const currentType = usePlatformTypeStore((state) => state.currentType);
    const variables = useWorkflowVariables();

    // `undefined` means this build has no variables feature at all (the CE edition-seam default) — render
    // nothing. `DataPillPanelBody` already skips this item's AccordionItem wrapper in that case; this guard
    // keeps the component correct in isolation too.
    if (variables === undefined) {
        return null;
    }

    const filteredVariables = variables.filter((variable) =>
        variable.name.toLowerCase().includes(dataPillFilterQuery.toLowerCase())
    );

    const variablesSettingsPath =
        currentType === PlatformType.EMBEDDED ? EMBEDDED_VARIABLES_SETTINGS_PATH : AUTOMATION_VARIABLES_SETTINGS_PATH;

    return (
        <>
            <AccordionTrigger className="group flex w-full items-center justify-between border-border/50 bg-surface-main p-4 group-data-[state=closed]:border-b">
                <div className="flex items-center space-x-4">
                    <VariableIcon className="size-5" />

                    <span className="text-sm">Variables</span>
                </div>

                <ChevronDownIcon className="size-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
            </AccordionTrigger>

            <AccordionContent className="size-full space-y-2 border-b border-b-border/50 px-4 pb-4">
                {variables.length === 0 ? (
                    <p className="text-sm">
                        <span>No variables defined.&nbsp;</span>

                        <Link className="text-content-brand-primary underline" to={variablesSettingsPath}>
                            Manage variables
                        </Link>
                    </p>
                ) : (
                    <ul className="flex w-full flex-col space-y-2 border-l border-l-border/50 pl-4 group-data-[state=open]:h-full">
                        {filteredVariables.map((variable) => (
                            <li className="flex w-full items-center space-x-3" key={variable.id}>
                                <DataPill
                                    parentProperty={{name: VARIABLES_NODE_NAME, type: 'OBJECT' as PropertyType}}
                                    path={variable.name}
                                    property={{name: variable.name, type: 'STRING' as PropertyType}}
                                    sampleOutput={variable.value}
                                    workflowNodeName={VARIABLES_NODE_NAME}
                                />
                            </li>
                        ))}
                    </ul>
                )}
            </AccordionContent>
        </>
    );
};

export default DataPillPanelBodyVariablesItem;
