import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './styles/global.css';
import {ThemeProvider} from "./contexts/ThemeContext.tsx";

ReactDOM
    .createRoot(document.getElementById('root')!)
    .render(
        <BrowserRouter>
            <ThemeProvider>
                <App />
            </ThemeProvider>
        </BrowserRouter>
    );
