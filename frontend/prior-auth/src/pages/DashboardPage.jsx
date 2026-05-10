import { useState, useEffect, useCallback, useMemo } from "react";
import { submitPrediction, fetchHistory } from "../api";

/* ── Suggestion data for real-time hints ──────────────────── */
const DIAGNOSIS_HINTS = [
  "Type 2 Diabetes", "Type 1 Diabetes", "Hypertension", "Asthma",
  "Fracture", "High Cholesterol", "Obesity", "Depression", "Anxiety",
  "Back Pain", "Knee Injury", "Cancer", "Heart Disease",
  "Kidney Disease", "Allergies",
];

const TREATMENT_HINTS = [
  "Insulin Therapy", "Physical Therapy", "Blood Test", "X-Ray",
  "Vaccination", "Antibiotics", "Routine Checkup", "MRI Scan",
  "CT Scan", "Ultrasound", "Echocardiogram", "Colonoscopy",
  "Psychiatric Evaluation", "Cardiac Stress Test", "Dialysis",
  "Cosmetic Surgery", "Elective Surgery", "Weight Loss Surgery",
];

/* ── Probability Ring ─────────────────────────────────────── */
function ProbabilityRing({ value, status }) {
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const pct = value / 100;
  const offset = circumference - pct * circumference;

  const color =
    status === "Likely Approved"
      ? "var(--green)"
      : status === "Needs Review"
        ? "var(--yellow)"
        : "var(--red)";

  return (
    <div className="prob-ring">
      <svg width="96" height="96" viewBox="0 0 96 96">
        <circle className="prob-ring__bg" cx="48" cy="48" r={radius} />
        <circle
          className="prob-ring__fill"
          cx="48" cy="48" r={radius}
          stroke={color}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
      </svg>
      <span className="prob-ring__text" style={{ color }}>
        {Math.round(value)}%
      </span>
    </div>
  );
}

/* ── Status Badge ─────────────────────────────────────────── */
function StatusBadge({ status }) {
  const variant =
    status === "Likely Approved" ? "approved"
      : status === "Needs Review" ? "review" : "denied";
  const emoji =
    status === "Likely Approved" ? "✓"
      : status === "Needs Review" ? "⚠" : "✕";

  return (
    <span className={`status-badge status-badge--${variant}`}>
      {emoji} {status}
    </span>
  );
}

/* ── Autocomplete Input ───────────────────────────────────── */
function AutocompleteInput({ id, name, value, onChange, hints, placeholder, required }) {
  const [focused, setFocused] = useState(false);

  const filtered = useMemo(() => {
    if (!value || value.length < 1) return [];
    return hints.filter((h) =>
      h.toLowerCase().includes(value.toLowerCase())
    ).slice(0, 5);
  }, [value, hints]);

  const showDropdown = focused && filtered.length > 0;

  return (
    <div className="autocomplete-wrapper">
      <input
        id={id}
        name={name}
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        onFocus={() => setFocused(true)}
        onBlur={() => setTimeout(() => setFocused(false), 150)}
        required={required}
        autoComplete="off"
      />
      {showDropdown && (
        <ul className="autocomplete-dropdown">
          {filtered.map((hint) => (
            <li
              key={hint}
              onMouseDown={() =>
                onChange({ target: { name, value: hint } })
              }
            >
              {hint}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/* ── Patient Form ─────────────────────────────────────────── */
function PatientForm({ onSubmit, loading }) {
  const [form, setForm] = useState({ 
    age: "", diagnosis: "", treatment: "", 
    provider: "", priority: "Routine", clinicalNotes: "", document: null 
  });
  const [touched, setTouched] = useState({});

  const handleChange = (e) => {
    const { name, value, type, files } = e.target;
    if (type === "file") {
      setForm((p) => ({ ...p, [name]: files[0] || null }));
    } else {
      setForm((p) => ({ ...p, [name]: value }));
    }
  };

  const handleBlur = (e) => {
    setTouched((p) => ({ ...p, [e.target.name]: true }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setTouched({ age: true, diagnosis: true, treatment: true, provider: true });
    if (!form.age || !form.diagnosis || !form.treatment || !form.provider) return;
    onSubmit(form);
  };

  const fieldError = (name) =>
    touched[name] && !form[name] ? "This field is required" : null;

  return (
    <form onSubmit={handleSubmit} id="patient-form">
      <div className="form-group">
        <label htmlFor="age">Patient Age</label>
        <input
          id="age"
          name="age"
          type="number"
          placeholder="e.g. 45"
          min="0"
          max="150"
          value={form.age}
          onChange={handleChange}
          onBlur={handleBlur}
          required
          className={fieldError("age") ? "input-error" : ""}
        />
        {fieldError("age") && <span className="field-error">{fieldError("age")}</span>}
      </div>

      <div className="form-group">
        <label htmlFor="diagnosis">Diagnosis</label>
        <AutocompleteInput
          id="diagnosis"
          name="diagnosis"
          value={form.diagnosis}
          onChange={handleChange}
          hints={DIAGNOSIS_HINTS}
          placeholder="Start typing… e.g. Type 2 Diabetes"
          required
        />
        {fieldError("diagnosis") && <span className="field-error">{fieldError("diagnosis")}</span>}
      </div>

      <div className="form-group">
        <label htmlFor="treatment">Requested Treatment</label>
        <AutocompleteInput
          id="treatment"
          name="treatment"
          value={form.treatment}
          onChange={handleChange}
          hints={TREATMENT_HINTS}
          placeholder="Start typing… e.g. Insulin Therapy"
          required
        />
        {fieldError("treatment") && <span className="field-error">{fieldError("treatment")}</span>}
      </div>

      <div className="form-row">
        <div className="form-group">
          <label htmlFor="provider">Insurance Provider</label>
          <AutocompleteInput
            id="provider"
            name="provider"
            value={form.provider}
            onChange={handleChange}
            hints={["Aetna", "Blue Cross Blue Shield", "Cigna", "UnitedHealthcare", "Humana", "Medicare", "Medicaid"]}
            placeholder="e.g. Aetna"
            required
          />
          {fieldError("provider") && <span className="field-error">{fieldError("provider")}</span>}
        </div>
        
        <div className="form-group">
          <label htmlFor="priority">Priority Level</label>
          <select id="priority" name="priority" value={form.priority} onChange={handleChange}>
            <option value="Routine">Routine</option>
            <option value="Urgent">Urgent</option>
            <option value="Emergency">Emergency</option>
          </select>
        </div>
      </div>

      <div className="form-group">
        <label htmlFor="clinicalNotes">Clinical Notes (Optional)</label>
        <textarea
          id="clinicalNotes"
          name="clinicalNotes"
          placeholder="Briefly describe the clinical justification..."
          value={form.clinicalNotes}
          onChange={handleChange}
          rows="3"
        />
      </div>

      <div className="form-group file-upload-group">
        <label htmlFor="document">Supporting Document (Form/Prescription)</label>
        <div className="file-input-wrapper">
          <input
            id="document"
            name="document"
            type="file"
            accept=".pdf,.png,.jpg,.jpeg,.doc,.docx"
            onChange={handleChange}
            className="file-input"
          />
          <div className="file-input-custom">
            <span className="file-input-icon">📎</span>
            <span className="file-input-text">
              {form.document ? form.document.name : "Choose a file or drag it here..."}
            </span>
          </div>
        </div>
      </div>

      <button type="submit" className="btn-primary" disabled={loading} id="submit-btn">
        {loading ? (
          <><span className="spinner" /> Analyzing…</>
        ) : (
          "🔍  Analyze Authorization"
        )}
      </button>
    </form>
  );
}

/* ── Result Panel ─────────────────────────────────────────── */
function ResultPanel({ result }) {
  if (!result) {
    return (
      <div className="result-empty">
        <div className="empty-icon">📋</div>
        <p>Submit a patient request to see the<br />AI-powered authorization prediction</p>
      </div>
    );
  }

  return (
    <div className="result-panel">
      <div className="result-top">
        <StatusBadge status={result.status} />
        <span className={`confidence-badge confidence--${result.confidenceLevel.toLowerCase()}`}>
          {result.confidenceLevel} Confidence
        </span>
      </div>

      <div className="prob-section">
        <ProbabilityRing value={result.approvalPercentage} status={result.status} />
        <div className="prob-info">
          <h3>Approval Probability</h3>
          <p>Age {result.age} · {result.diagnosis} · {result.treatment}</p>
        </div>
      </div>

      <div className="detail-section">
        <h4>Reasoning</h4>
        <ul className="reasons-list">
          {result.reasons.map((r, i) => (
            <li key={i}>{r}</li>
          ))}
        </ul>
      </div>

      <div className="detail-section">
        <h4>Suggestions</h4>
        <ul className="suggestions-list">
          {result.suggestions.map((s, i) => (
            <li key={i}>{s}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}

/* ── History Table ────────────────────────────────────────── */
function HistoryTable({ history }) {
  if (!history || history.length === 0) return null;

  const badgeClass = (status) =>
    status === "Likely Approved" ? "status-badge--approved"
      : status === "Needs Review" ? "status-badge--review" : "status-badge--denied";

  return (
    <div className="history-section">
      <div className="card">
        <h2 className="card__title">
          <span className="icon icon--green">📊</span>
          Recent Predictions
        </h2>
        <div className="history-table-wrapper">
          <table className="history-table" id="history-table">
            <thead>
              <tr>
                <th>Age</th>
                <th>Diagnosis</th>
                <th>Treatment</th>
                <th>Approval</th>
                <th>Confidence</th>
                <th>Status</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {history.map((item) => (
                <tr key={item.id}>
                  <td>{item.age}</td>
                  <td>{item.diagnosis}</td>
                  <td>{item.treatment}</td>
                  <td>{Math.round(item.approvalPercentage)}%</td>
                  <td>{item.confidenceLevel}</td>
                  <td>
                    <span className={`mini-badge ${badgeClass(item.status)}`}>
                      {item.status}
                    </span>
                  </td>
                  <td>{new Date(item.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ── AI Assistant Widget ──────────────────────────────────── */
function AIAssistantWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    { role: "assistant", content: "Hi! I'm Lumina, your AI Prior-Auth Assistant. How can I help you today? Need guidance on submitting a request?" }
  ]);
  const [input, setInput] = useState("");

  const handleSend = (e) => {
    e.preventDefault();
    if (!input.trim()) return;
    const userMsg = input.trim();
    setMessages(prev => [...prev, { role: "user", content: userMsg }]);
    setInput("");
    
    // Simulate AI response
    setTimeout(() => {
      let aiResponse = "I can guide you through the prior authorization process. Based on typical requirements, make sure to provide accurate diagnosis and treatment information.";
      if (userMsg.toLowerCase().includes("diabetes")) {
        aiResponse = "For diabetes treatments, insurance typically requires A1C test results from the last 3 months and documentation of previous therapies.";
      } else if (userMsg.toLowerCase().includes("mri") || userMsg.toLowerCase().includes("scan")) {
        aiResponse = "Imaging requests like MRIs often need clinical notes showing conservative treatments (like physical therapy) were tried first.";
      }
      setMessages(prev => [...prev, { role: "assistant", content: aiResponse }]);
    }, 1000);
  };

  return (
    <div className={`ai-assistant-widget ${isOpen ? "open" : ""}`}>
      {isOpen ? (
        <div className="ai-chat-window card">
          <div className="ai-chat-header">
            <span className="icon icon--blue">✨</span>
            <h3>Lumina AI</h3>
            <button className="ai-close-btn" onClick={() => setIsOpen(false)}>✕</button>
          </div>
          <div className="ai-chat-messages">
            {messages.map((msg, i) => (
              <div key={i} className={`ai-message ${msg.role}`}>
                <div className="ai-message-content">{msg.content}</div>
              </div>
            ))}
          </div>
          <form className="ai-chat-input-form" onSubmit={handleSend}>
            <input 
              type="text" 
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask for guidance..." 
            />
            <button type="submit" className="ai-send-btn">➤</button>
          </form>
        </div>
      ) : (
        <button className="ai-fab-btn pulse-button" onClick={() => setIsOpen(true)}>
          <span className="ai-fab-icon">✨</span>
          <span className="ai-fab-text">Ask Lumina</span>
        </button>
      )}
    </div>
  );
}

/* ── Dashboard Page ───────────────────────────────────────── */
export default function DashboardPage() {
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const loadHistory = useCallback(async () => {
    try {
      const data = await fetchHistory();
      setHistory(data);
    } catch { /* silent */ }
  }, []);

  useEffect(() => { loadHistory(); }, [loadHistory]);

  const handleSubmit = async (form) => {
    setLoading(true);
    setError("");
    setResult(null);
    try {
      const data = await submitPrediction(form);
      setResult(data);
      await loadHistory();
    } catch (err) {
      setError(err.message || "Something went wrong. Is the backend running?");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* Hero */}
      <header className="header modern-header">
        <div className="header__badge">
          <span className="pulse" />
          Powered by Lumina AI
        </div>
        <h1 className="modern-gradient-text">mediAuth Intelligence</h1>
        <p className="modern-subtitle">
          Next-generation AI predicting and explaining prior-authorization outcomes with clinical precision.
        </p>
      </header>

      {/* Main Grid */}
      <div className="main-grid">
        <div className="card">
          <h2 className="card__title">
            <span className="icon icon--blue">🏥</span>
            Patient Information
          </h2>
          {error && <div className="error-msg" id="error-msg">{error}</div>}
          <PatientForm onSubmit={handleSubmit} loading={loading} />
        </div>

        <div className="card">
          <h2 className="card__title">
            <span className="icon icon--green">📊</span>
            Prediction Result
          </h2>
          <ResultPanel result={result} />
        </div>
      </div>

      <HistoryTable history={history} />
      
      {/* AI Assistant Floating Widget */}
      <AIAssistantWidget />
    </>
  );
}
