import React, { useState } from 'react';
import { PageName } from './types';
import { useNotification } from './hooks/useNotification';
import { useAuth } from './hooks/useAuth';
import { authService } from './services/authService';
import { alarmService, GetAlarmResponse } from './services/alarmService';
import { userService } from './services/userService';
import { dashboardService, GetSleepDashboardResponse } from './services/dashboardService';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import HomePage from './pages/HomePage';
import SetAlarmPage from './pages/SetAlarmPage';

export default function App() {
    const [page, setPage] = useState<PageName>('login');
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [alarmTime, setAlarmTime] = useState('07:30');
    const [alarmData, setAlarmData] = useState<GetAlarmResponse | null>(null);
    const [nickname, setNickname] = useState<string>('');
    const [dashboardData, setDashboardData] = useState<GetSleepDashboardResponse | null>(null);
    const { notification, showNotification } = useNotification();
    const auth = useAuth();

    // 로그인
    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!userId || !password) {
            showNotification('아이디와 비밀번호를 입력해주세요.');
            return;
        }
        try {
            const res = await authService.login({ nickname: userId, password });
            auth.login(res.jwt, res.userId);
            fetchDashboardData(res.jwt);
            setPage('home');
            showNotification('로그인에 성공했습니다.');
        } catch (err) {
            showNotification(err instanceof Error ? err.message : '로그인에 실패했습니다.');
        }
    };

    // 회원가입
    const handleSignup = async (nickname: string, pw: string) => {
        try {
            const res = await authService.signUp({ nickname, password: pw });
            auth.login(res.jwt, res.userId);
            fetchDashboardData(res.jwt);
            setPage('home');
            showNotification('회원가입이 완료되었습니다.');
        } catch (err) {
            showNotification(err instanceof Error ? err.message : '회원가입에 실패했습니다.');
        }
    };

    // 통합 데이터 조회 (알람, 사용자, 대시보드)
    const fetchDashboardData = async (token: string) => {
        try {
            const [alarmRes, userRes, dashRes] = await Promise.all([
                alarmService.getAlarm(token).catch(() => null),
                userService.getUserProfile(token).catch(() => null),
                dashboardService.getSleepSummary(token).catch(() => null),
            ]);

            if (alarmRes) {
                setAlarmData(alarmRes);
                // 오늘 요일의 baseWakeTime을 기본 alarmTime으로 표시
                const today = alarmRes.alarms.find(a => a.dayOfWeek === alarmRes.todayDayOfWeek);
                if (today) setAlarmTime(today.baseWakeTime);
            }
            if (userRes) setNickname(userRes.nickname);
            if (dashRes) setDashboardData(dashRes);
        } catch {
            // 조회 실패 시 기본값 유지
        }
    };

    // 알람 설정 저장
    const handleSetAlarm = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!auth.token) return;
        try {
            const today = new Date().getDay();
            // JS getDay(): 0=일, 1=월 → ISO: 1=월 ... 7=일 변환
            const dayOfWeek = today === 0 ? 7 : today;
            const res = await alarmService.patchAlarm(
                { dayOfWeek, baseWakeTime: alarmTime },
                auth.token
            );
            setAlarmData(res);
            setPage('home');
            showNotification('알람이 성공적으로 설정되었습니다.');
        } catch (err) {
            showNotification(err instanceof Error ? err.message : '알람 설정에 실패했습니다.');
        }
    };

    // 로그아웃
    const handleLogout = () => {
        auth.logout();
        setUserId('');
        setPassword('');
        setAlarmData(null);
        setNickname('');
        setDashboardData(null);
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
                    alarmTime={alarmTime}
                    alarmData={alarmData}
                    nickname={nickname || 'DeepSleep'}
                    dashboardData={dashboardData}
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