import { useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav className="navbar" id="navbar">
      <div className="navbar__inner">
        <div className="navbar__brand">
          <span className="navbar__icon">🛡️</span>
          <span className="navbar__name">mediAuth</span>
          <span className="navbar__separator">|</span>
          <span className="navbar__tagline">Intelligence Platform</span>
        </div>

        {user && (
          <div className="navbar__right">
            <span className="navbar__user">
              <span className="navbar__avatar">
                {user.fullName?.charAt(0)?.toUpperCase() || "U"}
              </span>
              {user.fullName}
            </span>
            <button className="btn-logout" onClick={handleLogout} id="logout-btn">
              Logout
            </button>
          </div>
        )}
      </div>
    </nav>
  );
}
