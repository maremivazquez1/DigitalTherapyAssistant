import { Navigate, Outlet, useLocation } from "react-router-dom";
import { isAuthenticated } from '../services/auth/authService';

export function ProtectedRoute() {
  const location = useLocation();
  if (!isAuthenticated()) {
    // Redirect to /login but save current location to redirect back from login
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  // allow other routes if logged in
  return <Outlet />;
}
