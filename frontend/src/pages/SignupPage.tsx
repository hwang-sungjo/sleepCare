import React, { useState } from 'react';
import { User, Lock, CheckCircle2 } from 'lucide-react';
import PageWrapper from '../layouts/PageWrapper';
import Button from '../components/Button';
import InputField from '../components/InputField';

interface SignupPageProps {
    notification: string | null;
    onSignup: (userId: string, password: string, passwordConfirm: string) => void;
    onBack: () => void;
}

const USER_ID_REGEX = /^[a-z0-9]{5,20}$/;
const PASSWORD_REGEX = /^[a-z0-9]{8,20}$/;

function validateUserId(value: string): { hint: string; type: 'info' | 'error' | 'success' } {
    if (value.length === 0) return { hint: '영문 소문자와 숫자 조합, 5~20자', type: 'info' };
    if (/[A-Z]/.test(value)) return { hint: '대문자는 사용할 수 없습니다', type: 'error' };
    if (/[^a-z0-9]/.test(value)) return { hint: '영문 소문자와 숫자만 사용 가능합니다', type: 'error' };
    if (value.length < 5) return { hint: `${5 - value.length}자 더 입력해주세요 (최소 5자)`, type: 'error' };
    if (value.length > 20) return { hint: '20자 이하로 입력해주세요', type: 'error' };
    if (USER_ID_REGEX.test(value)) return { hint: '사용 가능한 아이디입니다 ✓', type: 'success' };
    return { hint: '영문 소문자와 숫자 조합, 5~20자', type: 'error' };
}

function validatePassword(value: string): { hint: string; type: 'info' | 'error' | 'success' } {
    if (value.length === 0) return { hint: '영문 소문자와 숫자 조합, 8~20자', type: 'info' };
    if (/[A-Z]/.test(value)) return { hint: '대문자는 사용할 수 없습니다', type: 'error' };
    if (/[^a-z0-9]/.test(value)) return { hint: '영문 소문자와 숫자만 사용 가능합니다', type: 'error' };
    if (value.length < 8) return { hint: `${8 - value.length}자 더 입력해주세요 (최소 8자)`, type: 'error' };
    if (value.length > 20) return { hint: '20자 이하로 입력해주세요', type: 'error' };
    if (PASSWORD_REGEX.test(value)) return { hint: '사용 가능한 비밀번호입니다 ✓', type: 'success' };
    return { hint: '영문 소문자와 숫자 조합, 8~20자', type: 'error' };
}

function validatePasswordConfirm(password: string, confirm: string): { hint: string; type: 'info' | 'error' | 'success' } {
    if (confirm.length === 0) return { hint: '위 비밀번호와 동일하게 입력해주세요', type: 'info' };
    if (password !== confirm) return { hint: '비밀번호가 일치하지 않습니다', type: 'error' };
    return { hint: '비밀번호가 일치합니다 ✓', type: 'success' };
}

const SignupPage: React.FC<SignupPageProps> = ({ notification, onSignup, onBack }) => {
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [passwordConfirm, setPasswordConfirm] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSignup(userId, password, passwordConfirm);
    };

    const userIdHint = validateUserId(userId);
    const passwordHint = validatePassword(password);
    const confirmHint = validatePasswordConfirm(password, passwordConfirm);

    return (
        <PageWrapper title="회원가입" showBack onBack={onBack} currentPage="signup" notification={notification}>
            <p className="text-slate-400 mb-8">
                당신의 더 나은 수면 경험을 위해
                <br />
                계정을 생성해주세요.
            </p>
            <form onSubmit={handleSubmit} className="flex flex-col flex-1">
                <InputField
                    label="아이디"
                    placeholder="영문 소문자+숫자, 5~20자"
                    icon={User}
                    value={userId}
                    onChange={(e) => setUserId(e.target.value)}
                    hint={userIdHint.hint}
                    hintType={userIdHint.type}
                />
                <InputField
                    label="비밀번호"
                    type="password"
                    placeholder="영문 소문자+숫자, 8~20자"
                    icon={Lock}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    hint={passwordHint.hint}
                    hintType={passwordHint.type}
                />
                <InputField
                    label="비밀번호 확인"
                    type="password"
                    placeholder="비밀번호 다시 입력"
                    icon={CheckCircle2}
                    value={passwordConfirm}
                    onChange={(e) => setPasswordConfirm(e.target.value)}
                    hint={confirmHint.hint}
                    hintType={confirmHint.type}
                />
                <div className="mt-auto">
                    <Button type="submit">계정 생성하기</Button>
                </div>
            </form>
        </PageWrapper>
    );
};

export default SignupPage;

