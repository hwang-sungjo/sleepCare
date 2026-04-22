import React from 'react';
import { User, Lock, CheckCircle2 } from 'lucide-react';
import PageWrapper from '../layouts/PageWrapper';
import Button from '../components/Button';
import InputField from '../components/InputField';

interface SignupPageProps {
    notification: string | null;
    onSignup: (e: React.FormEvent) => void;
    onBack: () => void;
}

const SignupPage: React.FC<SignupPageProps> = ({ notification, onSignup, onBack }) => (
    <PageWrapper title="회원가입" showBack onBack={onBack} currentPage="signup" notification={notification}>
        <p className="text-slate-400 mb-8">
            당신의 더 나은 수면 경험을 위해
            <br />
            계정을 생성해주세요.
        </p>
        <form onSubmit={onSignup} className="flex flex-col flex-1">
            <InputField label="아이디" placeholder="사용할 아이디 입력" icon={User} />
            <InputField label="비밀번호" type="password" placeholder="비밀번호 설정" icon={Lock} />
            <InputField label="비밀번호 확인" type="password" placeholder="비밀번호 다시 입력" icon={CheckCircle2} />
            <div className="mt-auto">
                <Button type="submit">계정 생성하기</Button>
            </div>
        </form>
    </PageWrapper>
);

export default SignupPage;
