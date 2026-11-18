import { Link, Route, Routes } from 'react-router-dom';
import CreateTicketPage from './pages/CreateTicketPage';
import TicketDetailPage from './pages/TicketDetailPage';
import TicketListPage from './pages/TicketListPage';

export default function App() {
  return (
    <div className="container">
      <header className="app-header">
        <Link to="/" className="brand">Support Tickets</Link>
        <Link to="/tickets/new" className="btn btn-primary">New ticket</Link>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<TicketListPage />} />
          <Route path="/tickets/new" element={<CreateTicketPage />} />
          <Route path="/tickets/:id" element={<TicketDetailPage />} />
        </Routes>
      </main>
    </div>
  );
}
