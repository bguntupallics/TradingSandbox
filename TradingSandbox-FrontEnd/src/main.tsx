import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './styles/global.css';
import {ThemeProvider} from "./contexts/ThemeContext.tsx";
import {AuthProvider} from "./contexts/AuthContext.tsx";

ReactDOM
    .createRoot(document.getElementById('root')!)
    .render(
        <BrowserRouter>
            <ThemeProvider>
                <AuthProvider>
                    <App />
                </AuthProvider>
            </ThemeProvider>
        </BrowserRouter>
    );
