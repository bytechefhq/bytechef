import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Input} from '@/components/ui/input';
import {useCreateJobExecutionsPackageCheckoutSessionMutation} from '@/shared/mutations/platform/billing.mutations';
import {useState} from 'react';
import {toast} from 'sonner';

interface BuyJobExecutionsPackageDialogPropsI {
    onClose: () => void;
    open: boolean;
}

const MAX_QUANTITY = 100;
const MIN_QUANTITY = 1;
const PACKAGE_PRICE_USD = 5;
const PACKAGE_UNITS = 1000;

const BuyJobExecutionsPackageDialog = ({onClose, open}: BuyJobExecutionsPackageDialogPropsI) => {
    const [quantityText, setQuantityText] = useState('1');

    const {isPending, mutate: createCheckoutSession} = useCreateJobExecutionsPackageCheckoutSessionMutation();

    const quantity = Number(quantityText);
    const isQuantityValid =
        quantityText.trim() !== '' &&
        Number.isInteger(quantity) &&
        quantity >= MIN_QUANTITY &&
        quantity <= MAX_QUANTITY;

    const handleConfirm = () => {
        createCheckoutSession(quantity, {
            onSuccess: (session) => {
                if (session.checkoutUrl) {
                    window.location.href = session.checkoutUrl;
                } else {
                    toast.error('Failed to start checkout. Please try again.');
                }
            },
        });
    };

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Extra units are valid for as long as your subscription is active."
                        title="Buy extra units"
                    />

                    <DialogBody>
                        <div className="flex flex-col gap-2">
                            <label className="text-sm font-medium" htmlFor="job-executions-package-quantity">
                                Quantity
                            </label>

                            <Input
                                id="job-executions-package-quantity"
                                max={MAX_QUANTITY}
                                min={MIN_QUANTITY}
                                onChange={(event) => setQuantityText(event.target.value)}
                                type="number"
                                value={quantityText}
                            />

                            <span className="text-sm text-muted-foreground">
                                {isQuantityValid
                                    ? `${(quantity * PACKAGE_UNITS).toLocaleString('en-US')} job executions for $${quantity * PACKAGE_PRICE_USD}`
                                    : `Enter a quantity between ${MIN_QUANTITY} and ${MAX_QUANTITY}.`}
                            </span>
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <Button label="Cancel" onClick={onClose} variant="outline" />

                        <Button
                            disabled={!isQuantityValid || isPending}
                            label={isPending ? 'Loading…' : 'Continue to checkout'}
                            onClick={handleConfirm}
                            variant="default"
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default BuyJobExecutionsPackageDialog;
