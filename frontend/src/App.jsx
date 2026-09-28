import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Tickets from './pages/Tickets'
import TicketDetails from './pages/TicketDetails'
import CreateTicket from './pages/CreateTicket'
import Users from './pages/Users'
import KnowledgeBase from './pages/KnowledgeBase'
import Profile from './pages/Profile'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/tickets" element={<Tickets />} />
          <Route path="/tickets/:id" element={<TicketDetails />} />
          <Route path="/tickets/new" element={<CreateTicket />} />
          <Route path="/knowledge-base" element={<KnowledgeBase />} />
          <Route path="/profile" element={<Profile />} />
          <Route element={<ProtectedRoute roles={['ADMIN']} />}>
            <Route path="/users" element={<Users />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
