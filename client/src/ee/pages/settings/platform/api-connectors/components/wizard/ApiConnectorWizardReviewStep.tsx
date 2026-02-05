import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Trash2Icon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import {WizardModeType} from '../../types/api-connector-wizard.types';
import {getHttpMethodBadgeColor} from '../../utils/httpMethod-utils';
import useApiConnectorWizardReviewStep from './hooks/useApiConnectorWizardReviewStep';

interface ApiConnectorWizardReviewStepProps {
    mode: WizardModeType;
}

const ApiConnectorWizardReviewStep = ({mode}: ApiConnectorWizardReviewStepProps) => {
    const {baseUrl, displayEndpoints, name, removeEndpoint, specification} = useApiConnectorWizardReviewStep({mode});

    return (
        <div className="flex flex-col gap-4 pb-4">
            <div>
                <h3 className="text-sm font-medium">Review API Connector</h3>

                <p className="text-sm text-muted-foreground">Review the endpoints before creating the API connector.</p>
            </div>

            <div className="rounded-md border p-3">
                <p className="text-sm">
                    <span className="font-medium">Name:</span> {name}
                </p>

                {baseUrl && (
                    <p className="text-sm">
                        <span className="font-medium">Base URL:</span> {baseUrl}
                    </p>
                )}

                <p className="text-sm">
                    <span className="font-medium">Endpoints:</span> {displayEndpoints.length}
                </p>
            </div>

            <div>
                <h4 className="mb-2 text-sm font-medium">Endpoints</h4>

                {displayEndpoints.length === 0 ? (
                    <div className="rounded-md border border-dashed p-4 text-center">
                        <p className="text-sm text-muted-foreground">No endpoints found.</p>
                    </div>
                ) : (
                    <ul className="max-h-64 divide-y overflow-y-auto rounded-md border">
                        {displayEndpoints.map((endpoint) => (
                            <li className="flex items-center justify-between p-3" key={endpoint.id}>
                                <div className="flex items-center gap-3">
                                    <Badge
                                        className={twMerge('w-20', getHttpMethodBadgeColor(endpoint.httpMethod))}
                                        label={endpoint.httpMethod}
                                        styleType="outline-outline"
                                        weight="semibold"
                                    />

                                    <div>
                                        <p className="text-sm font-medium">{endpoint.operationId}</p>

                                        <p className="text-xs text-gray-500">{endpoint.path}</p>

                                        {endpoint.summary && (
                                            <p className="text-xs text-gray-400">{endpoint.summary}</p>
                                        )}
                                    </div>
                                </div>

                                {mode === 'manual' && (
                                    <Button
                                        icon={<Trash2Icon className="size-4" />}
                                        onClick={() => removeEndpoint(endpoint.id)}
                                        size="icon"
                                        variant="ghost"
                                    />
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>

            {specification && (
                <div>
                    <h4 className="mb-2 text-sm font-medium">Generated OpenAPI Specification</h4>

                    <pre className="max-h-48 overflow-auto rounded-md bg-gray-100 p-3 text-xs">{specification}</pre>
                </div>
            )}
        </div>
    );
};

export default ApiConnectorWizardReviewStep;
