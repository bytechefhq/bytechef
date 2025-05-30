# @bytechef/use-embedded-connection-dialog

A React hook for managing embedded ByteChef connection dialogs.

## Installation

```bash
npm install @bytechef/use-embedded-connection-dialog
```

## Usage

```tsx
import useEmbeddedByteChefConnectionDialog from "@bytechef/use-embedded-connection-dialog";

const Component = () => {
    const { openDialog, closeDialog } = useEmbeddedByteChefConnectionDialog();

    return <button onClick={openDialog}>Open Dialog</button>;
};
```

## License

MIT
