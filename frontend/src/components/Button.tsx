import React from 'react';
import { ButtonProps } from '../types';

const Button: React.FC<ButtonProps> = ({
    children,
    onClick,
    variant = 'primary',
    className = '',
    type = 'button',
}) => {
    const baseStyles =
        'w-full py-4 rounded-2xl font-bold transition-all active:scale-95 flex items-center justify-center gap-2';
    const variants = {
        primary: 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/30 hover:bg-indigo-700',
        secondary: 'bg-slate-800 text-slate-300 border border-slate-700 hover:bg-slate-700',
        outline: 'bg-transparent text-indigo-400 border border-indigo-500/50 hover:bg-indigo-500/10',
    };

    return (
        <button type={type} onClick={onClick} className={`${baseStyles} ${variants[variant]} ${className}`}>
            {children}
        </button>
    );
};

export default Button;
