import { useState } from 'react';

export function useNotification() {
    const [notification, setNotification] = useState<string | null>(null);

    const showNotification = (msg: string) => {
        setNotification(msg);
        setTimeout(() => setNotification(null), 3000);
    };

    return { notification, showNotification };
}
