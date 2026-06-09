import React, { useState } from 'react';
import { BookOpen, ChevronDown, ChevronUp } from 'lucide-react';

export interface CitationItem {
    location: string | null;
    snippet: string | null;
}

interface Props {
    citations: CitationItem[];
    /** compact: 대시보드용 한 줄 요약 + 펼치기 */
    variant?: 'default' | 'compact';
}

const CitationList: React.FC<Props> = ({ citations, variant = 'default' }) => {
    const [open, setOpen] = useState(false);
    const valid = citations.filter((c) => c.snippet != null);
    if (valid.length === 0) return null;

    const buttonClass =
        variant === 'compact'
            ? 'flex items-center gap-1 text-xs text-indigo-300/50 hover:text-indigo-300/80 transition-colors'
            : 'flex items-center gap-1 text-xs text-slate-500 hover:text-slate-300 transition-colors px-1';

    return (
        <div className={variant === 'compact' ? 'mt-2' : 'mt-1.5 w-full'}>
            <button onClick={() => setOpen((o) => !o)} className={buttonClass}>
                <BookOpen size={11} />
                참고 문헌 {valid.length}건
                {open ? <ChevronUp size={11} /> : <ChevronDown size={11} />}
            </button>
            {open && (
                <div className={`space-y-1.5 ${variant === 'compact' ? 'mt-1.5' : 'mt-1.5'}`}>
                    {valid.map((c, idx) => (
                        <div
                            key={idx}
                            className="bg-slate-900/60 border border-slate-700/40 rounded-xl px-3 py-2"
                        >
                            {c.location && (
                                <p className="text-indigo-400/80 text-xs font-medium mb-1 truncate">
                                    📄 {c.location}
                                </p>
                            )}
                            <p className="text-slate-500 text-xs leading-relaxed line-clamp-2">
                                {c.snippet}
                            </p>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default CitationList;
