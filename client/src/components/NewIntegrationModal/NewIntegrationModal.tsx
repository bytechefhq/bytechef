import React from 'react';

class NewIntegrationModal extends React.Component<
	{},
	{
		text: string;
		area: string;
		category: string;
		tags: string;
	}
> {
	state = {
		text: '',
		area: '',
		category: '',
		tags: '',
	};

	onNameChange = (e: React.FormEvent<HTMLInputElement>): void => {
		this.setState({text: e.currentTarget.value});
	};

	handleAreaChange(event: {target: {value: any}}) {
		console.log(event.target.value);
	}

	onCategoryChange = (e: React.FormEvent<HTMLInputElement>): void => {
		this.setState({category: e.currentTarget.value});
	};

	onTagsChange = (e: React.FormEvent<HTMLInputElement>): void => {
		this.setState({tags: e.currentTarget.value});
	};

	render() {
		return (
			<>
				<form name="message" method="post">
					<label htmlFor="name">
						<h3 className="tracking-tight text-gray-900 dark:text-gray-200">
							Name
						</h3>

						<input
							style={{width: '90%', marginRight: '10px'}}
							id="name"
							type="text"
							value={this.state.text}
							onChange={this.onNameChange}
						/>

						<button
							type="button"
							className="inline-flex items-center rounded-md border border-transparent bg-black px-2 py-1 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:bg-sky-500 dark:hover:bg-sky-400"
						>
							Button
						</button>
					</label>
				</form>

				<div>
					<h3 className="tracking-tight text-gray-900 dark:text-gray-200">
						Description
					</h3>

					<textarea
						style={{width: '100%'}}
						name="textValue"
						onChange={this.handleAreaChange}
						rows={5}
						cols={60}
					/>
				</div>

				<div>
					<h3 className="tracking-tight text-gray-900 dark:text-gray-200">
						Category
					</h3>

					<input
						style={{width: '100%'}}
						type="text"
						value={this.state.category}
						onChange={this.onCategoryChange}
					/>
				</div>

				<div>
					<h3 className="tracking-tight text-gray-900 dark:text-gray-200">
						Tags
					</h3>

					<input
						style={{width: '100%'}}
						type="text"
						value={this.state.category}
						onChange={this.onTagsChange}
					/>
				</div>

				<label style={{display: 'flex', justifyContent: 'flex-end'}}>
					<button
						type="button"
						className="inline-flex items-center rounded-md border border-transparent bg-black px-2 py-1 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:bg-sky-500 dark:hover:bg-sky-400"
					>
						Cancel
					</button>
					&nbsp;
					<button
						type="button"
						className="flex-end right inline-felx items-center rounded-md border border-transparent bg-black px-2 py-1 text-base font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:bg-sky-500 dark:hover:bg-sky-400"
					>
						Save
					</button>
				</label>
			</>
		);
	}
}

export default NewIntegrationModal;
