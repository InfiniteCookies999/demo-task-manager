import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import "./index.css";
import { BrowserRouter } from 'react-router-dom';


/*
<StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,*/

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <App />
  </BrowserRouter>

)
