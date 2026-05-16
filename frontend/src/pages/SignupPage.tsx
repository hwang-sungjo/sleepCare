import React, { useState } from 'react';
import { User, Lock, CheckCircle2 } from 'lucide-react';
import PageWrapper from '../layouts/PageWrapper';
import Button from '../components/Button';
import InputField from '../components/InputField';

interface SignupPageProps {
    notification: string | null;
    onSignup: (nickname: string, password: string) => void;
    onBack: () => void;
}

const SignupPage: React.FC<SignupPageProps> = ({ notification, onSignup, onBack }) => {
    const [nickname, setNickname] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        if (!nickname.trim() || !password || !confirmPassword) {
            setError('모든 항목을 입력해주세요.');
            return;
        }
        if (password !== confirmPassword) {
            setError('비밀번호가 일치하지 않습니다.');
            return;
        }

        onSignup(nickname.trim(), password);
    };

    return (
        <PageWrapper title="회원가입" showBack onBack={onBack} currentPage="signup" notification={notification}>
            <p className="text-slate-400 mb-8">
                당신의 더 나은 수면 경험을 위해
                <br />
                계정을 생성해주세요.
            </p>
            {error && (
                <p className="text-red-400 text-sm mb-4 bg-red-500/10 px-4 py-2 rounded-xl">
                    {error}
                </p>
            )}
            <form onSubmit={handleSubmit} className="flex flex-col flex-1">
                <InputField
                    label="아이디"
                    placeholder="사용할 아이디 입력"
                    icon={User}
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                />
                <InputField
                    label="비밀번호"
                    type="password"
                    placeholder="비밀번호 설정"
                    icon={Lock}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                />
                <InputField
                    label="비밀번호 확인"
                    type="password"
                    placeholder="비밀번호 다시 입력"
                    icon={CheckCircle2}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                />
                <div className="mt-auto">
                    <Button type="submit">계정 생성하기</Button>
                </div>
            </form>
        </PageWrapper>
    );
};

export default SignupPage;
