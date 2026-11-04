import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Textarea} from '@/components/ui/textarea';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardIcon} from 'lucide-react';
import {toast} from 'sonner';

interface SigningSecretDialogProps {
    onClose: () => void;
    signingSecret: string;
}

const SigningSecretDialog = ({onClose, signingSecret}: SigningSecretDialogProps) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Use this secret to verify the signature of each webhook request."
                        title="Signing Secret"
                    />

                    <DialogBody>
                        <div className="space-y-4">
                            <p className="text-sm">Copy this secret now. You won&apos;t be able to see it again.</p>

                            <Textarea
                                className="field-sizing-fixed resize-none font-mono text-xs text-nowrap md:text-xs"
                                readOnly
                                rows={2}
                                value={signingSecret}
                            />

                            <div className="flex justify-end">
                                <Button
                                    icon={<ClipboardIcon />}
                                    label="Copy"
                                    onClick={() => {
                                        void copyToClipboard(signingSecret);

                                        toast('The signing secret is copied.');
                                    }}
                                    type="button"
                                />
                            </div>
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton label="Done" />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default SigningSecretDialog;
