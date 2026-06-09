import { useState } from 'react';
import { clearToken, getToken, setToken as persistToken } from '../api/client';

const USER_ID_KEY = 'sleepcare_user_id';

export function useAuth() {
    const [token, setToken] = useState<string | null>(() => getToken());
    const [userId, setUserId] = useState<number | null>(() => {
        const stored = localStorage.getItem(USER_ID_KEY);
        return stored ? Number(stored) : null;
    });

    const login = (jwt: string, id: number) => {
        persistToken(jwt);
        localStorage.setItem(USER_ID_KEY, String(id));
        setToken(jwt);
        setUserId(id);
    };

    const logout = () => {
        clearToken();
        localStorage.removeItem(USER_ID_KEY);
        setToken(null);
        setUserId(null);
    };

    return {
        token,
        userId,
        isLoggedIn: token !== null,
        login,
        logout,
    };
}
