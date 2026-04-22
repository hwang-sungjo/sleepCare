import React, { useState } from 'react';
import { PageName } from './types';
import { useNotification } from './hooks/useNotification';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import SetAlarmPage from './pages/SetAlarmPage';

export default function App() {
    const [page, setPage] = useState<PageName>('login');
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [alarmTime, setAlarmTime] = useState('07:30');
    const { notification, showNotification } = useNotification();

    const handleLogin = (e: React.FormEvent) => {
        e.preventDefault();
        if (userId && password) {
            setPage('home');
            showNotification('로그인에 성공했습니다.');
        } else {
            showNotification('아이디와 비밀번호를 입력해주세요.');
        }
    };

    const handleSignup = (e: React.FormEvent) => {
        e.preventDefault();
        showNotification('회원가입이 완료되었습니다.');
        setPage('login');
    };

    const handleSetAlarm = (e: React.FormEvent) => {
        e.preventDefault();
        showNotification('알람이 성공적으로 설정되었습니다.');
        setPage('home');
    };

    switch (page) {
        case 'login':
            return (
                <LoginPage
                    userId={userId}
                    password={password}
                    notification={notification}
                    onUserIdChange={setUserId}
                    onPasswordChange={setPassword}
                    onLogin={handleLogin}
                    onNavigate={setPage}
                />
            );
        case 'signup':
            return (
                <SignupPage
                    notification={notification}
                    onSignup={handleSignup}
                    onBack={() => setPage('login')}
                />
            );
        case 'home':
            return (
                <HomePage
                    alarmTime={alarmTime}
                    notification={notification}
                    onNavigate={setPage}
                />
            );
        case 'setAlarm':
            return (
                <SetAlarmPage
                    alarmTime={alarmTime}
                    notification={notification}
                    onAlarmTimeChange={setAlarmTime}
                    onSave={handleSetAlarm}
                    onBack={() => setPage('home')}
                />
            );
        default:
            return null;
    }
}