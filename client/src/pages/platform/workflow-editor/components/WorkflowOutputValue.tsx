import {ComponentDefinitionBasicModel} from '@/shared/middleware/platform/configuration';
import InlineSVG from 'react-inlinesvg';

const WorkflowOutputValue = ({
    componentDefinitions,
    value,
}: {
    componentDefinitions: Array<ComponentDefinitionBasicModel>;
    value: string;
}) => {
    const valueParts: Array<string> = value.split(/(\$\{.*?\})/g).filter((value: string) => value !== '');

    return (
        <div className="flex items-center whitespace-pre">
            {valueParts.map((part, index) => {
                if (part.startsWith('${')) {
                    const componentName = part.split('_')[0].replace('${', '');

                    const componentIcon =
                        componentDefinitions.find((component) => component.name === componentName)?.icon || '📄';

                    const partValue = part.replace(/\$\{|\}/g, '');

                    return (
                        <div
                            className="flex items-center rounded-full border bg-gray-100 px-2 py-0.5"
                            key={`${partValue}_${index}`}
                        >
                            <InlineSVG className="mr-2 size-4" src={componentIcon} />

                            <span>{partValue}</span>
                        </div>
                    );
                } else {
                    return <span key={part}>{part}</span>;
                }
            })}
        </div>
    );
};

export default WorkflowOutputValue;
