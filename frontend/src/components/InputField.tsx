import React from 'react';
import { InputFieldProps } from '../types';

const InputField: React.FC<InputFieldProps> = ({
    label,
    type = 'text',
    placeholder,
    value,
    onChange,
    icon: Icon,
}) => (
    <div className="flex flex-col gap-2 mb-4">
        <label className="text-sm text-slate-400 ml-1">{label}</label>
        <div className="relative group">
            {Icon && (
                <Icon
                    className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-indigo-400 transition-colors"
                    size={20}
                />
            )}
            <input
                type={type}
                value={value}
                onChange={onChange}
                placeholder={placeholder}
                className="w-full bg-slate-900 border border-slate-800 text-white rounded-2xl py-4 pl-12 pr-4 focus:outline-none focus:border-indigo-500 transition-all placeholder:text-slate-600"
            />
        </div>
    </div>
);

export default InputField;
