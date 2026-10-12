import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Checkbox} from '@/components/ui/checkbox';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {useOauth2ConsentClientQuery} from '@/shared/middleware/graphql';
import {ShieldCheckIcon} from 'lucide-react';
import {FormEvent, useMemo, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

const SCOPE_LABELS: Record<string, string> = {
    'mcp:automation': 'Access the Automation MCP server',
    'mcp:management': 'Access the Management MCP server',
};

const UNVERIFIED_CLIENT_NOTICE =
    "The application's name is self-declared and unverified. Only allow access if you trust where it sends you back: ";

const getScopeLabel = (scope: string): string => SCOPE_LABELS[scope] || scope;

const parseScopes = (scope: string | null): string[] => scope?.split(' ').filter(Boolean) ?? [];

const OAuth2Consent = () => {
    const [submitting, setSubmitting] = useState(false);
    const [uncheckedScopes, setUncheckedScopes] = useState<string[]>([]);

    const [searchParams] = useSearchParams();

    const clientId = searchParams.get('client_id');
    const scope = searchParams.get('scope');
    const state = searchParams.get('state');

    const {data: consentClientData} = useOauth2ConsentClientQuery({clientId: clientId ?? ''}, {enabled: !!clientId});

    const consentClient = consentClientData?.oauth2ConsentClient;

    const requestedScopes = useMemo(() => parseScopes(scope), [scope]);

    const checkedScopes = useMemo(
        () => requestedScopes.filter((requestedScope) => !uncheckedScopes.includes(requestedScope)),
        [requestedScopes, uncheckedScopes]
    );

    const toggleScope = (scopeValue: string, checked: boolean) => {
        setUncheckedScopes((previous) =>
            checked ? previous.filter((value) => value !== scopeValue) : [...previous, scopeValue]
        );
    };

    const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
        if (submitting) {
            event.preventDefault();

            return;
        }

        setSubmitting(true);
    };

    if (!clientId || !state) {
        return (
            <PublicLayoutContainer>
                <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                    <CardHeader className="p-0 pb-6">
                        <CardTitle className="self-center text-xl font-semibold text-content-neutral-primary">
                            Invalid consent request
                        </CardTitle>
                    </CardHeader>

                    <CardContent className="p-0">
                        <p className="text-sm text-content-neutral-secondary">
                            This authorization request is missing required information and cannot be processed.
                        </p>
                    </CardContent>
                </Card>
            </PublicLayoutContainer>
        );
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="flex flex-col items-center gap-2 p-0 pb-6">
                    <ShieldCheckIcon className="size-8 text-content-brand-primary" />

                    <CardTitle className="text-center text-xl font-semibold text-content-neutral-primary">
                        Authorize access
                    </CardTitle>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    <div className="flex flex-col gap-2">
                        <p className="text-sm text-content-neutral-secondary">
                            <span className="font-semibold text-content-neutral-primary">
                                {consentClient?.clientName || clientId}
                            </span>

                            <span> is requesting access to your ByteChef account.</span>
                        </p>

                        {consentClient && (
                            <p className="text-xs text-content-neutral-secondary">
                                <span>{UNVERIFIED_CLIENT_NOTICE}</span>

                                <span className="font-medium text-content-neutral-primary">
                                    {consentClient.redirectHosts.join(', ')}
                                </span>
                            </p>
                        )}
                    </div>

                    {requestedScopes.length > 0 && (
                        <fieldset className="flex flex-col gap-3 border-0 p-0">
                            <legend className="pb-2 text-sm font-medium text-content-neutral-primary">
                                Requested permissions
                            </legend>

                            {requestedScopes.map((requestedScope) => (
                                <div className="flex items-center gap-2" key={requestedScope}>
                                    <Checkbox
                                        checked={checkedScopes.includes(requestedScope)}
                                        disabled={submitting}
                                        id={`scope-${requestedScope}`}
                                        onCheckedChange={(checked) => toggleScope(requestedScope, checked === true)}
                                    />

                                    <label
                                        className="text-sm font-normal text-content-neutral-primary"
                                        htmlFor={`scope-${requestedScope}`}
                                    >
                                        {getScopeLabel(requestedScope)}
                                    </label>
                                </div>
                            ))}
                        </fieldset>
                    )}

                    <form action="/oauth2/authorize" method="post" onSubmit={handleSubmit}>
                        <input name="client_id" type="hidden" value={clientId} />

                        <input name="state" type="hidden" value={state} />

                        {checkedScopes.map((checkedScope) => (
                            <input key={checkedScope} name="scope" type="hidden" value={checkedScope} />
                        ))}

                        <Button
                            className="w-full"
                            disabled={submitting || checkedScopes.length === 0}
                            label="Allow"
                            size="lg"
                            type="submit"
                        />
                    </form>

                    <form action="/oauth2/authorize" method="post" onSubmit={handleSubmit}>
                        <input name="client_id" type="hidden" value={clientId} />

                        <input name="state" type="hidden" value={state} />

                        <Button
                            className="w-full"
                            disabled={submitting}
                            label="Deny"
                            size="lg"
                            type="submit"
                            variant="outline"
                        />
                    </form>
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default OAuth2Consent;
