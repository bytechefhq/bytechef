import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {TriangleAlertIcon} from 'lucide-react';
import * as React from 'react';

const IntegrationSettingsDeleteForm = () => {
    return (
        <Card className="w-full border-0 shadow-none">
            <CardHeader className="px-0">
                <CardTitle>Danger Zone</CardTitle>
            </CardHeader>

            <CardContent className="px-0">
                <Alert variant="destructive">
                    <div className="flex items-center gap-x-4">
                        <div>
                            <TriangleAlertIcon className="size-4" />
                        </div>

                        <div className="flex-auto">
                            <AlertTitle>Delete Integration</AlertTitle>

                            <AlertDescription>
                                <div className="flex items-center">
                                    <div className="flex-auto"> Once confirmed, this operation cannot be undone.</div>

                                    <div>
                                        <Button variant="destructive">Delete</Button>{' '}
                                    </div>
                                </div>
                            </AlertDescription>
                        </div>
                    </div>
                </Alert>
            </CardContent>
        </Card>
    );
};

export default IntegrationSettingsDeleteForm;
