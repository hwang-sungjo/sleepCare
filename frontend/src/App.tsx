import React, { useState } from 'react';
import { PageName } from './types';
import { useNotification } from './hooks/useNotification';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import SetAlarmPage from './pages/SetAlarmPage';
import { login as apiLogin, signup as apiSignup } from './api/auth';
import { ApiError, clearToken, setToken } from './api/client';

export default function App() {
    const [page, setPage] = useState<PageName>('login');
    const [nickname, setNickname] = useState('');
    const [password, setPassword] = useState('');
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
        if (!nickname || !password) {
            showNotification('닉네임과 비밀번호를 입력해주세요.');
            return;
        }
        try {
            const res = await apiLogin(nickname, password);
            setToken(res.jwt);
            setPage('home');
            showNotification('로그인에 성공했습니다.');
        } catch (err) {
            showError(err, '로그인 중 오류가 발생했습니다.');
        }
    };

    const handleSignup = async (
        signupNickname: string,
        signupPassword: string,
        signupPasswordConfirm: string,
        fitbitUserId: string,
        fitbitUserPassword: string,
    ) => {
        if (!signupNickname || !signupPassword) {
            showNotification('닉네임과 비밀번호를 입력해주세요.');
            return;
        }
        if (signupPassword !== signupPasswordConfirm) {
            showNotification('비밀번호가 일치하지 않습니다.');
            return;
        }
        try {
            const res = await apiSignup({
                nickname: signupNickname,
                password: signupPassword,
                fitbitUserId,
                fitbitUserPassword,
            });
            // 원격 백엔드는 회원가입 시 즉시 토큰을 발급합니다.
            setToken(res.jwt);
            setNickname(signupNickname);
            setPassword('');
            setPage('home');
            showNotification('회원가입이 완료되었습니다.');
        } catch (err) {
            showError(err, '회원가입 중 오류가 발생했습니다.');
        }
    };

    const handleLogout = () => {
        clearToken();
        setNickname('');
        setPassword('');
        setPage('login');
    };

    const handleAlarmSaved = (message: string) => {
        showNotification(message);
        setPage('home');
    };

    switch (page) {
        case 'login':
            return (
                <LoginPage
                    nickname={nickname}
                    password={password}
                    notification={notification}
                    onNicknameChange={setNickname}
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
                    nickname={nickname}
                    notification={notification}
                    onNavigate={setPage}
                    onLogout={handleLogout}
                    onNicknameLoaded={setNickname}
                />
            );
        case 'setAlarm':
            return (
                <SetAlarmPage
                    notification={notification}
                    onSaved={handleAlarmSaved}
                    onBack={() => setPage('home')}
                />
            );
        default:
            return null;
    }
}
