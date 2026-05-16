import { useState } from 'react';

const TOKEN_KEY = 'sleepcare_jwt';
const USER_ID_KEY = 'sleepcare_user_id';

export function useAuth() {
    const [token, setToken] = useState<string | null>(
        () => localStorage.getItem(TOKEN_KEY)
    );
    const [userId, setUserId] = useState<number | null>(() => {
        const stored = localStorage.getItem(USER_ID_KEY);
        return stored ? Number(stored) : null;
    });

    const login = (jwt: string, id: number) => {
        localStorage.setItem(TOKEN_KEY, jwt);
        localStorage.setItem(USER_ID_KEY, String(id));
        setToken(jwt);
        setUserId(id);
    };

    const logout = () => {
        localStorage.removeItem(TOKEN_KEY);
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
