import React from 'react';
import { Moon, User, Lock } from 'lucide-react';
import { PageName } from '../types';
import PageWrapper from '../layouts/PageWrapper';
import Button from '../components/Button';
import InputField from '../components/InputField';

interface LoginPageProps {
    nickname: string;
    password: string;
    notification: string | null;
    onNicknameChange: (value: string) => void;
    onPasswordChange: (value: string) => void;
    onLogin: (e: React.FormEvent) => void;
    onNavigate: (page: PageName) => void;
}

const LoginPage: React.FC<LoginPageProps> = ({
    nickname,
    password,
    notification,
    onNicknameChange,
    onPasswordChange,
    onLogin,
    onNavigate,
}) => (
    <PageWrapper title="반가워요!" currentPage="login" notification={notification}>
        <div className="mt-8 flex flex-col items-center mb-12">
            <div className="w-20 h-20 bg-indigo-600 rounded-3xl flex items-center justify-center mb-6 shadow-xl shadow-indigo-500/20">
                <Moon className="text-white" size={40} />
            </div>
            <p className="text-slate-400 text-center">
                웨어러블 기반 스마트 알람 서비스
                <br />
                DeepSleep에 오신 것을 환영합니다.
            </p>
        </div>
        <form onSubmit={onLogin} className="flex flex-col flex-1">
            <InputField
                label="닉네임"
                placeholder="닉네임 입력"
                icon={User}
                value={nickname}
                onChange={(e) => onNicknameChange(e.target.value)}
            />
            <InputField
                label="비밀번호"
                type="password"
                placeholder="••••••••"
                icon={Lock}
                value={password}
                onChange={(e) => onPasswordChange(e.target.value)}
            />
            <div className="mt-auto space-y-4">
                <Button type="submit">로그인</Button>
                <Button variant="outline" onClick={() => onNavigate('signup')}>
                    회원가입
                </Button>
            </div>
        </form>
    </PageWrapper>
);

export default LoginPage;
