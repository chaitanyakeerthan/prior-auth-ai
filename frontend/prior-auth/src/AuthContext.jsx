import { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Hydrate from localStorage on mount
  useEffect(() => {
    const token = localStorage.getItem("xaip_token");
    const email = localStorage.getItem("xaip_email");
    const fullName = localStorage.getItem("xaip_name");
    if (token && email) {
      setUser({ token, email, fullName });
    }
    setLoading(false);
  }, []);

  const login = ({ token, email, fullName }) => {
    localStorage.setItem("xaip_token", token);
    localStorage.setItem("xaip_email", email);
    localStorage.setItem("xaip_name", fullName);
    setUser({ token, email, fullName });
  };

  const logout = () => {
    localStorage.removeItem("xaip_token");
    localStorage.removeItem("xaip_email");
    localStorage.removeItem("xaip_name");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
