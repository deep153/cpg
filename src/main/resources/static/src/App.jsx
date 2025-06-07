import React, { useState } from 'react';
import Editor from '@monaco-editor/react';
import './App.css';

function App() {
    const [code, setCode] = useState('');
    const [isAnalyzing, setIsAnalyzing] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);

    const handleAnalyze = async () => {
        if (!code.trim()) {
            setError('Code cannot be empty');
            return;
        }

        setIsAnalyzing(true);
        setError(null);
        setResult(null);

        try {
            const response = await fetch('/analyze', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    code: code,
                    page: 1
                })
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Analysis failed');
            }

            setResult(data.result);
        } catch (err) {
            setError(err.message);
        } finally {
            setIsAnalyzing(false);
        }
    };

    return (
        <div className="container">
            <div className="editor-panel">
                <h2>CPG Analysis</h2>
                <div className="editor-container">
                    <Editor
                        height="70vh"
                        defaultLanguage="rust"
                        value={code}
                        onChange={setCode}
                        theme="vs-dark"
                        options={{
                            minimap: { enabled: false },
                            fontSize: 14,
                            scrollBeyondLastLine: false,
                            automaticLayout: true
                        }}
                    />
                </div>
                <button 
                    className="analyze-button"
                    onClick={handleAnalyze}
                    disabled={isAnalyzing}
                >
                    {isAnalyzing ? 'Analyzing...' : 'Analyze Code'}
                </button>
                
                {error && (
                    <div className="message error">
                        {error}
                    </div>
                )}
                
                {result && (
                    <div className="message success">
                        {result}
                    </div>
                )}
            </div>
        </div>
    );
}

export default App; 