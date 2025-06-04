# @bytechef-embedded/react

ByteChef Embedded React SDK.

## Installation

```bash
npm install @bytechef-embedded/react
```

## Usage

```tsx
import useConnectDialog from '@bytechef-embedded/react';

const Component = () => {
    const {openDialog, closeDialog} = useConnectDialog();

    return <button onClick={openDialog}>Open Dialog</button>;
};
```

## Developing

1. Run SDK in watch mode

```bash
cd react
npm install
npm run run watch
```

2. Run the corresponding test server of your library from the previous step to see SDK project in action:

```bash
cd test-apps/react
npm link @bytechef/use-embedded-connection-dialog
npm install
npm run dev
```

## License

MIT
