import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {
    Arrow,
    Content,
    Provider,
    Root,
    Trigger,
    Portal,
} from '@radix-ui/react-tooltip';
import Button from 'components/Button/Button';
import Select from '../../../../components/Select/Select';
import useRightSlideOverStore from '../stores/useRightSlideOverStore';

const Tooltip = () => (
    <Provider>
        <Root>
            <Trigger asChild>
                <InfoCircledIcon className="ml-2 h-4 w-4" />
            </Trigger>

            <Portal>
                <Content
                    sideOffset={4}
                    // eslint-disable-next-line tailwindcss/no-custom-classname
                    className="z-50 inline-flex items-center rounded-md bg-gray-800 px-4 py-2.5 radix-side-bottom:animate-slide-up-fade radix-side-left:animate-slide-right-fade radix-side-right:animate-slide-left-fade radix-side-top:animate-slide-down-fade dark:bg-gray-800"
                >
                    <Arrow className="fill-current text-gray-800 dark:text-gray-800" />

                    <span className="block text-xs leading-none text-gray-100 dark:text-gray-100">
                        Information
                    </span>
                </Content>
            </Portal>
        </Root>
    </Provider>
);

const RightSlideOver = () => {
    const {currentNode, rightSlideOverOpen, setRightSlideOverOpen} =
        useRightSlideOverStore();

    return (
        <Dialog.Root
            open={rightSlideOverOpen}
            onOpenChange={() => setRightSlideOverOpen(!rightSlideOverOpen)}
            modal={false}
        >
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-0 right-0 z-10 w-screen max-w-md overflow-hidden bg-white"
                    onInteractOutside={(event) => event.preventDefault()}
                >
                    <Dialog.Title className="flex content-center items-center p-4 text-lg font-medium text-gray-900">
                        {currentNode.label}

                        <Tooltip />

                        <Button
                            aria-label="Close panel"
                            className="ml-auto"
                            displayType="icon"
                            icon={
                                <Cross1Icon
                                    className="h-3 w-3 cursor-pointer text-gray-900"
                                    aria-hidden="true"
                                />
                            }
                            onClick={() => setRightSlideOverOpen(false)}
                        />
                    </Dialog.Title>

                    <div className="flex h-full flex-col divide-y divide-gray-100 bg-white shadow-xl">
                        <div className="flex h-full flex-col space-y-4 p-4">
                            <div>
                                <label
                                    htmlFor="location"
                                    className="block text-sm font-medium text-gray-700"
                                >
                                    Location
                                </label>

                                <select
                                    id="location"
                                    name="location"
                                    className="mt-1 block w-full rounded-md border-gray-300 py-2 pl-3 pr-10 text-base focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
                                    defaultValue="Canada"
                                >
                                    <option>United States</option>
                                    <option>Canada</option>
                                    <option>Mexico</option>
                                </select>
                            </div>

                            <div className="flex justify-center space-x-1">
                                <Button label="Description" />

                                <Button label="Auth" />

                                <Button label="Properties" />

                                <Button label="Output" />
                            </div>

                            <div>
                                <label
                                    htmlFor="email"
                                    className="block text-sm font-medium text-gray-700"
                                >
                                    Email
                                </label>

                                <div className="mt-1">
                                    <input
                                        type="email"
                                        name="email"
                                        id="email"
                                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                                        placeholder="you@example.com"
                                    />
                                </div>
                            </div>

                            <div>
                                <label
                                    htmlFor="comment"
                                    className="block text-sm font-medium text-gray-700"
                                >
                                    Add your comment
                                </label>

                                <div className="mt-1">
                                    <textarea
                                        rows={4}
                                        name="comment"
                                        id="comment"
                                        className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                                        defaultValue={''}
                                    />
                                </div>
                            </div>
                        </div>

                        <div className="flex shrink-0 justify-start p-4">
                            <Select
                                defaultValue={'2'}
                                options={[
                                    {label: 'v 1', value: '1'},
                                    {label: 'v 2', value: '2'},
                                    {label: 'v 3', value: '3'},
                                ]}
                            />
                        </div>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default RightSlideOver;
