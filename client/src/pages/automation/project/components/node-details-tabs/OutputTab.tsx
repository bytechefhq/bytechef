/* eslint-disable @typescript-eslint/no-explicit-any */
import {
    CodeBracketSquareIcon,
    HashtagIcon,
    LanguageIcon,
    ListBulletIcon,
    VariableIcon,
} from '@heroicons/react/24/outline';
import {ActionDefinitionModel} from 'middleware/definition-registry';

const TYPE_ICONS = {
    STRING: <LanguageIcon className="h-5 w-5 text-gray-600" />,
    BOOLEAN: <VariableIcon className="h-5 w-5 text-gray-600" />,
    INTEGER: <HashtagIcon className="h-5 w-5 text-gray-600" />,
    ARRAY: <ListBulletIcon className="h-5 w-5 text-gray-600" />,
    OBJECT: <CodeBracketSquareIcon className="h-5 w-5 text-gray-600" />,
};

const SchemaProperties = ({properties}: {properties: any}) => {
    return (
        <ul className="h-full">
            {properties.map((property: any, index: number) => {
                return (
                    <li
                        className="flex flex-col"
                        key={`${property.name}-${index}`}
                    >
                        <div className="inline-flex rounded-md p-2 text-sm hover:bg-gray-100">
                            <span>
                                {
                                    TYPE_ICONS[
                                        property.type as keyof typeof TYPE_ICONS
                                    ]
                                }
                            </span>

                            <span className="pl-2">{property.label}</span>
                        </div>

                        {property.properties &&
                            !!property.properties.length && (
                                <div
                                    key={property.name}
                                    className="mb-4 ml-4 flex flex-col border-l-2 border-gray-300"
                                >
                                    <SchemaProperties
                                        properties={property.properties}
                                    />
                                </div>
                            )}
                    </li>
                );
            })}
        </ul>
    );
};

const OutputTab = ({action}: {action: ActionDefinitionModel}) => {
    return (
            {action.outputSchema?.map((schema: any) =>
        <div className="max-h-full flex-[1_1_1px] overflow-auto">
                schema.properties ? (
                    <SchemaProperties
                        key={schema.name}
                        properties={schema.properties}
                    />
                ) : (
                    <div
                        className="inline-flex rounded-md p-2 text-sm hover:bg-gray-100"
                        key={schema.name}
                    >
                        <span>
                            {TYPE_ICONS[schema.type as keyof typeof TYPE_ICONS]}
                        </span>

                        <span className="pl-2">{schema.controlType}</span>
                    </div>
                )
            )}
        </div>
    );
};

export default OutputTab;
