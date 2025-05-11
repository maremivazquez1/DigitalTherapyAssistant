import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";

const NavBar: React.FC = () => {
  const navigate = useNavigate();
  // keep login‐state in React so NavBar will re-render on logout
  const [isLoggedIn, setIsLoggedIn] = useState(
    Boolean(localStorage.getItem("authToken"))
  );

  const handleLogout = () => {
    localStorage.removeItem("authToken");
    setIsLoggedIn(false);     // force NavBar to re-render
    navigate("/login");
  };

  return (
    <div className="navbar bg-base-200 shadow-sm">
      <div className="flex-1">
        <Link to="/" className="btn btn-ghost text-xl">
          Digital Therapy Assistant
        </Link>
      </div>

      {isLoggedIn && (
        <div className="flex-none">
          <div className="dropdown dropdown-end dropdown-hover">
            <label tabIndex={0} className="btn btn-square btn-ghost">
              {/* …hamburger icon… */}
            </label>
            <ul
              tabIndex={0}
              className="dropdown-content menu p-2 shadow bg-base-200 rounded-box w-56"
            >
              <li>
                <Link to="/profile">Profile</Link>
              </li>
              <li>
                <button onClick={handleLogout} className="flex items-center">
                  Logout
                </button>
              </li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

export default NavBar;
