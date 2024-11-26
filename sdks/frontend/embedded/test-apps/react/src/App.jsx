import { AutoComplete, ConnectDialog, Todos } from '@bytechef-embedded/library-react';
import React, { useState } from 'react';

function App() {
  const [isOpen, setOpen] = useState(false);

  return (
    <div>
      <button onClick={() => setOpen(true)}>Connect</button>

      {isOpen && <ConnectDialog onClose={() => setOpen(false)} />}
    </div>
  );
}

export default App;
