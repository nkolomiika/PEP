import React from "react";
import ReactDOM from "react-dom/client";
import "./styles.css";

const phases = [
  "Теория OWASP Top 10",
  "Docker-подготовка",
  "Публикация Docker image",
  "Техническая проверка",
  "White box отчет",
  "Проверка куратора",
  "Запуск lab в kind",
  "Black box распределение",
  "Итоговая оценка"
];

const roles = [
  {
    name: "Студент",
    items: ["Docker-курс", "Сдача Docker image", "White box отчет", "Black box отчеты"]
  },
  {
    name: "Куратор",
    items: ["Очередь отчетов", "Проверка по чеклисту", "Баллы", "Обратная связь"]
  },
  {
    name: "Администратор",
    items: ["Курсы", "Группы", "Дедлайны", "Lab monitoring", "Audit log"]
  }
];

function App() {
  return (
    <main className="page">
      <section className="hero">
        <p className="eyebrow">Practical Exploitation Platform</p>
        <h1>Образовательная платформа по информационной безопасности</h1>
        <p>
          Полный цикл обучения: Docker, OWASP Top 10, публикация уязвимого приложения в виде
          Docker image, white box отчет, запуск lab в локальном kind и black box тестирование.
        </p>
      </section>

      <section className="grid">
        {roles.map((role) => (
          <article className="card" key={role.name}>
            <h2>{role.name}</h2>
            <ul>
              {role.items.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </article>
        ))}
      </section>

      <section className="card">
        <h2>Жизненный цикл модуля</h2>
        <ol className="timeline">
          {phases.map((phase) => (
            <li key={phase}>{phase}</li>
          ))}
        </ol>
      </section>
    </main>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
