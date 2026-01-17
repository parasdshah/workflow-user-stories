import { MantineProvider } from '@mantine/core';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import WorkflowList from './pages/WorkflowList';
import WorkflowEditor from './pages/WorkflowEditor';
import DeploymentHistory from './pages/DeploymentHistory';
import TaskInbox from './pages/TaskInbox';
import UserWorkload from './pages/UserWorkload';
import DiagramPreview from './pages/DiagramPreview';
import { ScreenDefinitionList } from './components/screens/ScreenDefinitionList';
import { ScreenDefinitionEditor } from './components/screens/ScreenDefinitionEditor';
import CaseView from './pages/CaseView';
import AuditLog from './pages/AuditLog';
import RuleManagement from './pages/RuleManagement';
import ModuleMaster from './pages/ModuleMaster';
import { HrmsConsole } from './pages/HrmsConsole';
import Header from './components/Header';

function App() {
  return (
    <MantineProvider>
      <BrowserRouter>
        <Header />
        <Routes>
          <Route path="/" element={<WorkflowList />} />
          <Route path="/create" element={<WorkflowEditor />} />
          <Route path="/edit/:code" element={<WorkflowEditor />} />
          <Route path="/deployments" element={<DeploymentHistory />} />
          <Route path="/inbox" element={<TaskInbox />} />
          <Route path="/workload" element={<UserWorkload />} />
          <Route path="/preview/:code" element={<DiagramPreview />} />
          <Route path="/screens" element={<ScreenDefinitionList />} />
          <Route path="/screens/:code" element={<ScreenDefinitionEditor />} />
          <Route path="/cases/:id" element={<CaseView />} />
          <Route path="/audit" element={<AuditLog />} />
          <Route path="/rules" element={<RuleManagement />} />
          <Route path="/modules" element={<ModuleMaster />} />
          <Route path="/admin/hrms" element={<HrmsConsole />} />
        </Routes>
      </BrowserRouter>
    </MantineProvider>
  );
}

export default App;
