import {Root, Portal, Overlay, Content, Close} from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';
import {useState} from 'react';
import './NewIntegrationButton.css';

const NewIntegrationButton = () => {
	const [openDialog, setOpenDialog] = useState(false);

	const handleOpenDialog = () => {
		setOpenDialog(true);
	};

	const handleCloseDialog = () => {
		setOpenDialog(false);
	};

	return (
		<>
			<button
				className="Button violet"
				onClick={() => handleOpenDialog()}
			>
				New Integration
			</button>

			{openDialog && (
				<Root open={openDialog}>
					<Portal>
						<Overlay className="DialogOverlay" />

						<Content className="DialogContent">
							<Close asChild>
								<button
									className="IconButton"
									aria-label="Close"
									onClick={() => handleCloseDialog()}
								>
									<Cross2Icon />
								</button>
							</Close>
						</Content>
					</Portal>
				</Root>
			)}
		</>
	);
};

export default NewIntegrationButton;
