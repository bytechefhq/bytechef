import Button from '@/components/Button/Button';
import {AlertCircle} from 'lucide-react';
import {Component, ErrorInfo, ReactNode} from 'react';

interface Props {
    children: ReactNode;
    fallback?: ReactNode;
    onReset?: () => void;
}

interface StateI {
    hasError: boolean;
    error?: Error;
}

/**
 * Error boundary component to catch JavaScript errors in its child component tree.
 * Displays a fallback UI instead of crashing the whole app.
 */
class ErrorBoundary extends Component<Props, StateI> {
    constructor(props: Props) {
        super(props);

        this.state = {hasError: false};
    }

    static getDerivedStateFromError(error: Error): StateI {
        return {error, hasError: true};
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
        console.error('ErrorBoundary caught an error:', error, errorInfo);
    }

    handleReset = (): void => {
        this.setState({error: undefined, hasError: false});
        this.props.onReset?.();
    };

    render(): ReactNode {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }

            return (
                <div className="flex size-full flex-col items-center justify-center p-8">
                    <div className="max-w-md space-y-4 text-center">
                        <AlertCircle className="mx-auto size-12 text-destructive" />

                        <h2 className="text-lg font-semibold">Something went wrong</h2>

                        <p className="text-sm text-muted-foreground">
                            {this.state.error?.message || 'An unexpected error occurred while rendering this component'}
                        </p>

                        <Button label="Try again" onClick={this.handleReset} variant="outline" />
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
