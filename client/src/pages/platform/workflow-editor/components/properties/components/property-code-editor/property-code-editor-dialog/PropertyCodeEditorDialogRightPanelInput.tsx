import Button from '@/components/Button/Button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {usePropertyCodeEditorDialogRightPanelInput} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/hooks/usePropertyCodeEditorDialogRightPanelInput';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {RotateCcwIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

type InputValueType = Record<string, unknown>;

interface PropertyCodeEditorDialogRightPanelInputProps {
    input: InputValueType;
}

const PropertyCodeEditorDialogRightPanelInput = ({input}: PropertyCodeEditorDialogRightPanelInputProps) => {
    const {handleEditorChange, handleReset, hasChanges, jsonValue, parseError} =
        usePropertyCodeEditorDialogRightPanelInput({input});

    return (
        <Card className="flex h-full flex-col border-none shadow-none">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 px-4 py-4">
                <div className="flex h-5 w-full items-center justify-between">
                    <CardTitle>Input</CardTitle>

                    {hasChanges && (
                        <Button
                            icon={<RotateCcwIcon />}
                            onClick={handleReset}
                            size="icon"
                            title="Reset to original"
                            variant="ghost"
                        />
                    )}
                </div>
            </CardHeader>

            <CardContent className="-ml-2 flex min-h-0 flex-1 flex-col pb-4 pl-0 pr-4">
                {Object.keys(input).length > 0 ? (
                    <div className="flex min-h-0 flex-1 flex-col">
                        <div className="min-h-0 flex-1 overflow-hidden" data-testid="input-editor-container">
                            <Suspense fallback={<MonacoEditorLoader />}>
                                <MonacoEditor
                                    className="size-full"
                                    defaultLanguage="json"
                                    onChange={handleEditorChange}
                                    onMount={(editor) => {
                                        editor.updateOptions({
                                            folding: true,
                                            lineNumbers: 'off',
                                            minimap: {enabled: false},
                                            scrollBeyondLastLine: false,
                                        });
                                    }}
                                    value={jsonValue}
                                />
                            </Suspense>
                        </div>

                        {parseError && <div className="mt-2 text-sm text-destructive">{parseError}</div>}
                    </div>
                ) : (
                    <div>
                        <span className="text-sm text-muted-foreground">No defined entries</span>
                    </div>
                )}
            </CardContent>
        </Card>
    );
};

export default PropertyCodeEditorDialogRightPanelInput;
