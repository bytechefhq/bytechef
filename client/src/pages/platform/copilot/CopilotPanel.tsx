import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {useCopilotStore} from '@/pages/platform/copilot/stores/useCopilotStore';
import {Cross2Icon} from '@radix-ui/react-icons';
import {ArrowUpIcon, BotMessageSquareIcon} from 'lucide-react';
import React from 'react';

const CopilotPanel = () => {
    const {setShowCopilot} = useCopilotStore();

    return (
        <div className="relative flex h-full min-h-[50vh] w-[450px] flex-col rounded-xl bg-muted/50 p-3 lg:col-span-2">
            <div className="mb-4 flex items-center justify-between">
                <div className="flex items-center space-x-1">
                    <BotMessageSquareIcon className="size-6" /> <h4>AI Copilot</h4>
                </div>

                <Button onClick={() => setShowCopilot(false)} size="icon" variant="ghost">
                    <Cross2Icon />
                </Button>
            </div>

            <div className="flex-1 overflow-auto pb-0.5">
                <div className="space-y-4">
                    <div className="rounded-lg bg-card text-card-foreground shadow-sm">
                        <div className="flex flex-col space-y-1.5 p-6">
                            <div className="flex items-center">
                                <div className="flex size-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                                    <span>U</span>
                                </div>

                                <h3 className="ml-2 text-sm font-semibold">User</h3>
                            </div>

                            <p className="text-sm text-muted-foreground">
                                Hello! I&apos;m interested in learning about machine learning. Can you give me a brief
                                introduction?
                            </p>
                        </div>
                    </div>

                    <div className="rounded-lg bg-card text-card-foreground shadow-sm">
                        <div className="flex flex-col space-y-1.5 p-6">
                            <div className="flex items-center">
                                <div className="flex size-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                                    <span>A</span>
                                </div>

                                <h3 className="ml-2 text-sm font-semibold">Assistant</h3>
                            </div>

                            <p className="text-sm text-muted-foreground">
                                <span>
                                    Machine learning is a branch of artificial intelligence that focuses on the
                                    development of algorithms and statistical models that enable computer systems to
                                    improve their performance on a specific task through experience, without being
                                    explicitly programmed.
                                </span>

                                <br />

                                <br />

                                <span>
                                    In essence, machine learning allows computers to learn from and make predictions or
                                    decisions based on data. It&apos;s used in a wide variety of applications,
                                    including:
                                </span>

                                <br />

                                <br />

                                <span> 1. Image and speech recognition</span>

                                <br />

                                <span>2. Natural language processing</span>

                                <br />

                                <span>3. Recommendation systems</span>

                                <br />

                                <span>4. Autonomous vehicles</span>

                                <br />

                                <span>5. Fraud detection</span>

                                <br />

                                <br />

                                <span>There are three main types of machine learning:</span>

                                <br />

                                <span>1. Supervised learning</span>

                                <br />

                                <span>2. Unsupervised learning</span>

                                <br />

                                <span>3. Reinforcement learning</span>

                                <br />

                                <br />

                                <span>
                                    Each type has its own set of algorithms and use cases. Is there a specific aspect of
                                    machine learning you&apos;d like to know more about?
                                </span>
                            </p>
                        </div>
                    </div>

                    <div className="rounded-lg bg-card text-card-foreground shadow-sm">
                        <div className="flex flex-col space-y-1.5 p-6">
                            <div className="flex items-center">
                                <div className="flex size-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                                    <span>U</span>
                                </div>

                                <h3 className="ml-2 text-sm font-semibold">User</h3>
                            </div>

                            <p className="text-sm text-muted-foreground">
                                That&apos;s really helpful, thank you! Can you explain a bit more about supervised
                                learning?
                            </p>
                        </div>
                    </div>

                    <div className="rounded-lg bg-card text-card-foreground shadow-sm">
                        <div className="flex flex-col space-y-1.5 p-6">
                            <div className="flex items-center">
                                <div className="flex size-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                                    <span>A</span>
                                </div>

                                <h3 className="ml-2 text-sm font-semibold">Assistant</h3>
                            </div>

                            <p className="text-sm text-muted-foreground">
                                <span>
                                    Supervised learning is one of the most common types of machine learning. Here&apos;s
                                    a brief explanation:
                                </span>

                                <br />

                                <span>
                                    Supervised learning is a method where the algorithm learns from labeled training
                                    data. In other words, the input data comes with the correct answers (labels), and
                                    the algorithm learns to predict the output from the input data.
                                </span>

                                <br />

                                <span>Key points about supervised learning:</span>

                                <br />

                                <span>
                                    1. Training Data: It consists of input features and their corresponding correct
                                    output labels.
                                </span>

                                <br />

                                <span>
                                    2. Goal: The algorithm aims to learn a function that can map new, unseen inputs to
                                    correct outputs.
                                </span>

                                <br />

                                <span>3. Types of Problems:</span>

                                <br />

                                <span>- Classification: Predicting a category (e.g., spam detection in emails)</span>

                                <br />

                                <span>- Regression: Predicting a continuous value (e.g., house prices)</span>

                                <br />

                                <span>
                                    4. Common Algorithms: Linear Regression, Logistic Regression, Decision Trees, Random
                                    Forests, Support Vector Machines, and Neural Networks.
                                </span>

                                <br />

                                <span>
                                    5. Evaluation: The model&apos;s performance is typically measured by how well it
                                    predicts outcomes for new, unseen data.
                                </span>

                                <br />

                                <br />

                                <span>
                                    An example of supervised learning could be an email spam filter. The algorithm would
                                    be trained on a dataset of emails labeled as &apos;spam&apos; or &apos;not
                                    spam&apos;. It learns the characteristics of spam emails and can then classify new,
                                    unseen emails.
                                </span>

                                <br />

                                <span>
                                    Is there anything specific about supervised learning you&apos;d like me to elaborate
                                    on?
                                </span>
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            <form className="relative mt-4 overflow-hidden rounded-lg border bg-background focus-within:ring-1 focus-within:ring-ring">
                <Label className="sr-only" htmlFor="message">
                    Message
                </Label>

                <Textarea
                    className="h-11 min-h-11 resize-none border-0 p-3 shadow-none focus-visible:ring-0"
                    id="message"
                    placeholder="Ask a question..."
                />

                <div className="absolute right-2 top-2 pt-0">
                    <Button className="ml-auto gap-1.5" size="sm" type="submit" variant="ghost">
                        <ArrowUpIcon className="size-3.5" />
                    </Button>
                </div>
            </form>
        </div>
    );
};

export default CopilotPanel;
