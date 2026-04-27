import React, { useState } from 'react';
import { PageName } from './types';
import { useNotification } from './hooks/useNotification';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import SetAlarmPage from './pages/SetAlarmPage';
import { login as apiLogin, signup as apiSignup } from './api/auth';
import { upsertAlarm as apiUpsertAlarm } from './api/alarm';
import { ApiError, clearToken, setToken } from './api/client';

export default function App() {
    const [page, setPage] = useState<PageName>('login');
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [userName, setUserName] = useState('');
    const [alarmTime, setAlarmTime] = useState('07:30');
    const { notification, showNotification } = useNotification();

    const showError = (e: unknown, fallback: string) => {
        if (e instanceof ApiError) {
            showNotification(e.message);
        } else {
            showNotification(fallback);
        }
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!userId || !password) {
            showNotification('아이디와 비밀번호를 입력해주세요.');
            return;
        }
        try {
            const res = await apiLogin(userId, password);
            setToken(res.token);
            setUserName(res.userName);
            setPage('home');
            showNotification('로그인에 성공했습니다.');
        } catch (err) {
            showError(err, '로그인 중 오류가 발생했습니다.');
        }
    };

    const handleSignup = async (signupUserId: string, signupPassword: string, signupPasswordConfirm: string) => {
        if (!signupUserId || !signupPassword) {
            showNotification('아이디와 비밀번호를 입력해주세요.');
            return;
        }
        if (signupPassword !== signupPasswordConfirm) {
            showNotification('비밀번호가 일치하지 않습니다.');
            return;
        }
        try {
            await apiSignup(signupUserId, signupPassword);
            showNotification('회원가입이 완료되었습니다. 로그인 해주세요.');
            setPage('login');
        } catch (err) {
            showError(err, '회원가입 중 오류가 발생했습니다.');
        }
    };

    const handleSetAlarm = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const res = await apiUpsertAlarm(alarmTime);
            showNotification(res.message);
            setPage('home');
        } catch (err) {
            showError(err, '알람 설정 중 오류가 발생했습니다.');
        }
    };

    const handleLogout = () => {
        clearToken();
        setUserId('');
        setPassword('');
        setUserName('');
        setPage('login');
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
                    userName={userName}
                    notification={notification}
                    onNavigate={setPage}
                    onLogout={handleLogout}
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
