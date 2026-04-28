import React, { useState } from 'react';
import { User, Lock, CheckCircle2, Watch, Key } from 'lucide-react';
import PageWrapper from '../layouts/PageWrapper';
import Button from '../components/Button';
import InputField from '../components/InputField';

interface SignupPageProps {
    notification: string | null;
    onSignup: (
        nickname: string,
        password: string,
        passwordConfirm: string,
        fitbitUserId: string,
        fitbitUserPassword: string,
    ) => void;
    onBack: () => void;
}

const NICKNAME_REGEX = /^[a-zA-Z0-9_]{4,20}$/;

function validateNickname(value: string): { hint: string; type: 'info' | 'error' | 'success' } {
    if (value.length === 0) return { hint: '영문/숫자/_ 조합, 4~20자', type: 'info' };
    if (/[^a-zA-Z0-9_]/.test(value)) return { hint: '영문, 숫자, 밑줄(_)만 사용 가능합니다', type: 'error' };
    if (value.length < 4) return { hint: `${4 - value.length}자 더 입력해주세요 (최소 4자)`, type: 'error' };
    if (value.length > 20) return { hint: '20자 이하로 입력해주세요', type: 'error' };
    if (NICKNAME_REGEX.test(value)) return { hint: '사용 가능한 닉네임입니다 ✓', type: 'success' };
    return { hint: '영문/숫자/_ 조합, 4~20자', type: 'error' };
}

function validatePassword(value: string): { hint: string; type: 'info' | 'error' | 'success' } {
    if (value.length === 0) return { hint: '8~30자, 대소문자·숫자·특수문자 자유 조합', type: 'info' };
    if (value.length < 8) return { hint: `${8 - value.length}자 더 입력해주세요 (최소 8자)`, type: 'error' };
    if (value.length > 30) return { hint: '30자 이하로 입력해주세요', type: 'error' };
    return { hint: '사용 가능한 비밀번호입니다 ✓', type: 'success' };
}

function validatePasswordConfirm(password: string, confirm: string): { hint: string; type: 'info' | 'error' | 'success' } {
    if (confirm.length === 0) return { hint: '위 비밀번호와 동일하게 입력해주세요', type: 'info' };
    if (password !== confirm) return { hint: '비밀번호가 일치하지 않습니다', type: 'error' };
    return { hint: '비밀번호가 일치합니다 ✓', type: 'success' };
}

const SignupPage: React.FC<SignupPageProps> = ({ notification, onSignup, onBack }) => {
    const [nickname, setNickname] = useState('');
    const [password, setPassword] = useState('');
    const [passwordConfirm, setPasswordConfirm] = useState('');
    const [fitbitUserId, setFitbitUserId] = useState('');
    const [fitbitUserPassword, setFitbitUserPassword] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSignup(nickname, password, passwordConfirm, fitbitUserId, fitbitUserPassword);
    };

    const nicknameHint = validateNickname(nickname);
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
                    label="닉네임"
                    placeholder="영문/숫자/_ 4~20자"
                    icon={User}
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                    hint={nicknameHint.hint}
                    hintType={nicknameHint.type}
                />
                <InputField
                    label="비밀번호"
                    type="password"
                    placeholder="8~30자"
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

                <div className="mt-2 mb-4 px-4 py-3 rounded-2xl bg-indigo-900/20 border border-indigo-500/20">
                    <p className="text-xs text-indigo-200/80">
                        Fitbit 계정 연동 (선택) — 수면 데이터 자동 동기화에 사용됩니다.
                    </p>
                </div>
                <InputField
                    label="Fitbit 아이디"
                    placeholder="(선택) Fitbit 계정 ID"
                    icon={Watch}
                    value={fitbitUserId}
                    onChange={(e) => setFitbitUserId(e.target.value)}
                />
                <InputField
                    label="Fitbit 비밀번호"
                    type="password"
                    placeholder="(선택) Fitbit 계정 비밀번호"
                    icon={Key}
                    value={fitbitUserPassword}
                    onChange={(e) => setFitbitUserPassword(e.target.value)}
                />

                <div className="mt-auto">
                    <Button type="submit">계정 생성하기</Button>
                </div>
            </form>
        </PageWrapper>
    );
};

export default SignupPage;
