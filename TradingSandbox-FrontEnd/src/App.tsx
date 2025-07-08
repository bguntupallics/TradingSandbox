import { Routes, Route } from 'react-router-dom';
import HomeRedirect from './components/HomeRedirect';
import RequireAuth from './components/RequireAuth';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import {LoggedInLayout} from "./components/LoggedInLayout.tsx";
import SearchPage from "./pages/SearchPage.tsx";
import AccountPage from "./pages/AccountPage.tsx";

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<HomeRedirect />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route
                path="/dashboard"
                element={
                    <RequireAuth>
                        <LoggedInLayout>
                            <Dashboard />
                        </LoggedInLayout>
                    </RequireAuth>
                }
            />
            <Route path="/search"    element={
                <RequireAuth>
                    <LoggedInLayout>
                        <SearchPage />
                    </LoggedInLayout>
                </RequireAuth>
            } />
            <Route path="/account"    element={
                <RequireAuth>
                    <LoggedInLayout>
                        <AccountPage />
                    </LoggedInLayout>
                </RequireAuth>
            } />
            <Route path="*" element={
                <div>404: Not Found</div>
            } />
        </Routes>
    );
}
