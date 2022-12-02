import React from 'react';

class NewIntegrationModal extends React.Component<
	{},
	{
		// no props
		text: string;
	}
> {
	state = {
		text: '',
	};

	onChange = (e: React.FormEvent<HTMLInputElement>): void => {
		this.setState({text: e.currentTarget.value});
	};

	render() {
		return (
			<form name="message" method="post">
				<label htmlFor="name">
					Name
					<input
						id="name"
						type="text"
						value={this.state.text}
						onChange={this.onChange}
					/>
					<button
						type="button"
						className="inline-flex items-center rounded-md border border-transparent bg-black px-2 py-1 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:bg-sky-500 dark:hover:bg-sky-400"
					>
						Button
					</button>
				</label>
			</form>
		);
	}
}

export default NewIntegrationModal;
