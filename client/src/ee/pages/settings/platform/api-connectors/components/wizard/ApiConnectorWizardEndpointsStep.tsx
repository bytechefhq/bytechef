import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {PlusIcon, Trash2Icon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import {getHttpMethodBadgeColor} from '../../utils/httpMethod-utils';
import {EndpointForm} from '../endpoint-editor';
import useApiConnectorWizardEndpointsStep from './hooks/useApiConnectorWizardEndpointsStep';

const ApiConnectorWizardEndpointsStep = () => {
    const {
        editingEndpoint,
        endpoints,
        handleCloseDialog,
        handleSaveEndpoint,
        isDialogOpen,
        openAddDialog,
        openEditDialog,
        removeEndpoint,
    } = useApiConnectorWizardEndpointsStep();

    return (
        <div className="flex flex-col gap-4 pb-4">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-medium">Endpoints</h3>

                <Button icon={<PlusIcon className="size-4" />} onClick={openAddDialog} size="sm" variant="secondary">
                    Add Endpoint
                </Button>
            </div>

            {endpoints.length === 0 ? (
                <div className="rounded-md border border-dashed p-8 text-center">
                    <p className="text-sm text-muted-foreground">No endpoints defined yet.</p>

                    <p className="text-sm text-muted-foreground">Click "Add Endpoint" to create your first endpoint.</p>
                </div>
            ) : (
                <ul className="divide-y rounded-md border">
                    {endpoints.map((endpoint) => (
                        <li
                            aria-label={`Edit ${endpoint.operationId} endpoint`}
                            className="flex cursor-pointer items-center justify-between p-3 hover:bg-gray-50 focus-visible:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-gray-400"
                            key={endpoint.id}
                            onClick={() => openEditDialog(endpoint)}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter' || event.key === ' ') {
                                    event.preventDefault();
                                    openEditDialog(endpoint);
                                }
                            }}
                            role="button"
                            tabIndex={0}
                        >
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

                                    <div className="mt-1 flex gap-2 text-xs text-gray-400">
                                        {endpoint.parameters.length > 0 && (
                                            <span>{endpoint.parameters.length} param(s)</span>
                                        )}

                                        {endpoint.requestBody && <span>body</span>}

                                        {endpoint.responses.length > 0 && (
                                            <span>{endpoint.responses.length} response(s)</span>
                                        )}
                                    </div>
                                </div>
                            </div>

                            <Button
                                icon={<Trash2Icon className="size-4" />}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    removeEndpoint(endpoint.id);
                                }}
                                size="icon"
                                variant="ghost"
                            />
                        </li>
                    ))}
                </ul>
            )}

            <EndpointForm
                endpoint={editingEndpoint}
                onClose={handleCloseDialog}
                onSave={handleSaveEndpoint}
                open={isDialogOpen}
            />
        </div>
    );
};

export default ApiConnectorWizardEndpointsStep;
