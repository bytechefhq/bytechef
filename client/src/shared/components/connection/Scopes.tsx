import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Label} from '@/components/ui/label';
import {ChevronDownIcon} from 'lucide-react';

interface ScopesProps {
    onSelectedScopesChange: (scopes: {[key: string]: boolean}) => void;
    scopeDefinitions: {[key: string]: boolean};
    selectedScopes?: {[key: string]: boolean};
}

const Scopes = ({onSelectedScopesChange, scopeDefinitions, selectedScopes = {}}: ScopesProps) => {
    const requiredScopeKeys = Object.keys(scopeDefinitions).filter((scope) => scopeDefinitions[scope]);

    const optionalScopeKeys = Object.keys(scopeDefinitions).filter((scope) => !scopeDefinitions[scope]);

    const handleScopeChange = (scope: string, checked: boolean) => {
        onSelectedScopesChange({
            ...scopeDefinitions,
            ...selectedScopes,
            [scope]: checked,
        });
    };

    return (
        <div className="py-2">
            <div className="flex flex-col space-y-2">
                <h2 className="font-semibold">Scopes</h2>

                <p className="text-sm text-muted-foreground">OAuth permission scopes used for this connection.</p>
            </div>

            {!!requiredScopeKeys.length && (
                <>
                    <h3 className="mt-2">Required Scopes</h3>

                    <ul className="space-y-2 p-2">
                        {requiredScopeKeys.map((scope) => (
                            <li className="flex items-center space-x-1" key={scope}>
                                <Checkbox
                                    checked={!!selectedScopes[scope]}
                                    id={scope}
                                    onCheckedChange={(checked) => handleScopeChange(scope, !!checked)}
                                />

                                <Label className="cursor-pointer" htmlFor={scope}>
                                    {scope}
                                </Label>
                            </li>
                        ))}
                    </ul>
                </>
            )}

            {!!optionalScopeKeys.length && (
                <Collapsible className="group mt-4 flex w-full flex-col justify-center">
                    <CollapsibleTrigger asChild>
                        <Button variant="outline">
                            <span>Show Optional Scopes</span>

                            <ChevronDownIcon className="size-4 transition-all group-data-[state=open]:rotate-180" />
                        </Button>
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                        <ul className="mt-2 space-y-2 py-2">
                            {optionalScopeKeys.map((scope) => (
                                <li className="flex w-full items-center space-x-1" key={scope}>
                                    <Checkbox
                                        checked={!!selectedScopes[scope]}
                                        id={scope}
                                        onCheckedChange={(checked) => handleScopeChange(scope, !!checked)}
                                    />

                                    <Label className="cursor-pointer" htmlFor={scope}>
                                        {scope}
                                    </Label>
                                </li>
                            ))}
                        </ul>
                    </CollapsibleContent>
                </Collapsible>
            )}
        </div>
    );
};

export default Scopes;
