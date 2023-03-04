import {Content, Root, Trigger} from '@radix-ui/react-hover-card';

interface NameProps {
    description: string;
    name: string;
}

const Name = ({description, name}: NameProps) => (
    <Root>
        <Trigger asChild>
            <span className="mr-2 text-base font-semibold text-gray-900">
                {name}
            </span>
        </Trigger>

        <Content
            align="center"
            className="max-w-md rounded-lg bg-white p-4 shadow-lg dark:bg-gray-800 md:w-full"
            sideOffset={4}
        >
            <div className="flex h-full w-full space-x-4">
                <p className="mt-1 text-sm font-normal text-gray-700 dark:text-gray-400">
                    {description}
                </p>
            </div>
        </Content>
    </Root>
);

export default Name;
