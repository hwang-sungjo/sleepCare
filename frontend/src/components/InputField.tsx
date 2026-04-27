import React, { useId } from 'react';
import { InputFieldProps } from '../types';

const InputField: React.FC<InputFieldProps> = ({
    label,
    type = 'text',
    placeholder,
    value,
    onChange,
    icon: Icon,
    hint,
    hintType = 'info',
}) => {
    const inputId = useId();

    const hintColors = {
        info: 'text-slate-500',
        error: 'text-rose-400',
        success: 'text-emerald-400',
    };

    const borderColor =
        hintType === 'error' && hint
            ? 'border-rose-500/50 focus:border-rose-500'
            : 'border-slate-800 focus:border-indigo-500';

    return (
        <div className="flex flex-col gap-2 mb-4">
            <label htmlFor={inputId} className="text-sm text-slate-400 ml-1">{label}</label>
            <div className="relative group">
                {Icon && (
                    <Icon
                        className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-indigo-400 transition-colors"
                        size={20}
                    />
                )}
                <input
                    id={inputId}
                    type={type}
                    value={value}
                    onChange={onChange}
                    placeholder={placeholder}
                    className={`w-full bg-slate-900 border ${borderColor} text-white rounded-2xl py-4 pl-12 pr-4 focus:outline-none transition-all placeholder:text-slate-600`}
                />
            </div>
            {hint && (
                <p className={`text-xs ml-1 ${hintColors[hintType]} transition-colors`}>
                    {hint}
                </p>
            )}
        </div>
    );
};

export default InputField;
