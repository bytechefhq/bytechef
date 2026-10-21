import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {toast} from 'sonner';

interface AppSecretKeyViewProps {
    secretKey: string;
}

const AppSecretKeyView = ({secretKey}: AppSecretKeyViewProps) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const environmentVariables = `BYTECHEF_URL=${window.location.origin}\nBYTECHEF_APP_KEY=${secretKey}`;

    return (
        <div className="space-y-4">
            <p className="text-sm">Save this key now. ByteChef cannot show it again; rotate the key if you lose it.</p>

            <div className="flex space-x-1">
                <Input aria-label="Secret key" readOnly={true} value={secretKey} />

                <Button
                    icon={<ClipboardIcon />}
                    label="Copy"
                    onClick={(event) => {
                        event.preventDefault();

                        copyToClipboard(secretKey);

                        toast('The app key is copied.');
                    }}
                />
            </div>

            <div className="space-y-1">
                <p className="text-sm font-medium">Paste these into the host&apos;s environment variables</p>

                <div className="flex items-start space-x-1">
                    <pre className="flex-1 overflow-x-auto rounded-md border border-input p-3 text-xs">
                        {environmentVariables}
                    </pre>

                    <Button
                        icon={<ClipboardIcon />}
                        label="Copy"
                        onClick={(event) => {
                            event.preventDefault();

                            copyToClipboard(environmentVariables);

                            toast('The environment variables are copied.');
                        }}
                    />
                </div>
            </div>
        </div>
    );
};

export default AppSecretKeyView;
