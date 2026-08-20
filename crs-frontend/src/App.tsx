import { useEffect, useState } from 'react';
import { getCourses } from './api/courseApi';
import type { Course } from './types/course';
import './index.css';

function App() {
  const [courses, setCourses] = useState<Course[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCourses()
      .then((res) => {
        setCourses(res.data.content);
        setError(null);
      })
      .catch(() => {
        setError(
          'Khong ket noi duoc toi he thong. Kiem tra lai api-gateway da chay chua.'
        );
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <div style={{ padding: '2rem', fontFamily: 'monospace' }}>
      <h1>Kiem tra ket noi CRS qua Gateway</h1>
      <p>
        <strong>API Base URL:</strong> {import.meta.env.VITE_API_BASE_URL}
      </p>
      <hr />
      {loading && <p>Dang tai du lieu...</p>}
      {error && (
        <p style={{ color: 'red' }}>
          <strong>Loi:</strong> {error}
        </p>
      )}
      {!loading && !error && (
        <>
          <p style={{ color: 'green' }}>
            Ket noi thanh cong! Nhan duoc {courses.length} khoa hoc.
          </p>
          <pre
            style={{
              background: '#1e1e1e',
              color: '#d4d4d4',
              padding: '1rem',
              borderRadius: '8px',
              overflow: 'auto',
              maxHeight: '600px',
            }}
          >
            {JSON.stringify(courses, null, 2)}
          </pre>
        </>
      )}
    </div>
  );
}

export default App;
