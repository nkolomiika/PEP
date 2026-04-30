const http = require("node:http");
const { URLSearchParams } = require("node:url");

const port = Number(process.env.PORT || 8080);

const users = [
  { email: "alice@example.local", password: "password", role: "student" },
  { email: "bob@example.local", password: "password", role: "student" },
  { email: "admin@example.local", password: "admin", role: "admin" },
];

function sendJson(response, statusCode, payload) {
  const body = JSON.stringify(payload, null, 2);
  response.writeHead(statusCode, {
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(body),
  });
  response.end(body);
}

function sendHtml(response, statusCode, body) {
  response.writeHead(statusCode, { "content-type": "text/html; charset=utf-8" });
  response.end(body);
}

function parseBody(request) {
  return new Promise((resolve, reject) => {
    let body = "";
    request.on("data", (chunk) => {
      body += chunk;
      if (body.length > 1_000_000) {
        request.destroy();
        reject(new Error("Request body is too large"));
      }
    });
    request.on("end", () => resolve(new URLSearchParams(body)));
    request.on("error", reject);
  });
}

function looksLikeBooleanSqli(value) {
  return /'\s*or\s*'1'\s*=\s*'1/i.test(value) || /'\s*or\s+1\s*=\s*1/i.test(value);
}

function unsafeLoginQuery(email, password) {
  return `SELECT * FROM users WHERE email = '${email}' AND password = '${password}'`;
}

function unsafeSearchQuery(query) {
  return `SELECT * FROM products WHERE title LIKE '%${query}%'`;
}

const server = http.createServer(async (request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);

  if (request.method === "GET" && url.pathname === "/health") {
    return sendJson(response, 200, { status: "UP" });
  }

  if (request.method === "GET" && url.pathname === "/") {
    return sendHtml(
      response,
      200,
      `<h1>vulnerable-sqli-demo</h1>
<p>Демонстрационное приложение для модуля A03. Injection.</p>
<p>Endpoints: <code>POST /login</code>, <code>GET /search?q=</code>, <code>GET /health</code>.</p>`
    );
  }

  if (request.method === "POST" && url.pathname === "/login") {
    const form = await parseBody(request);
    const email = form.get("email") || "";
    const password = form.get("password") || "";
    const sql = unsafeLoginQuery(email, password);
    const bypassed = looksLikeBooleanSqli(email) || looksLikeBooleanSqli(password);
    const user = bypassed || users.some((candidate) => candidate.email === email && candidate.password === password);

    if (!user) {
      return sendJson(response, 401, {
        authenticated: false,
        sql,
        hint: "Попробуйте payload ' OR '1'='1 в поле email.",
      });
    }

    return sendJson(response, 200, {
      authenticated: true,
      sql,
      warning: "Запрос собран небезопасной конкатенацией строк.",
    });
  }

  if (request.method === "GET" && url.pathname === "/search") {
    const query = url.searchParams.get("q") || "";
    const sql = unsafeSearchQuery(query);
    const injected = looksLikeBooleanSqli(query);
    const results = injected
      ? ["internal-admin-note", "draft-student-report", "public-lesson"]
      : ["public-lesson"];

    return sendJson(response, 200, {
      query,
      sql,
      results,
      warning: injected ? "Payload расширил результат поиска." : "Поиск выполнен.",
    });
  }

  return sendJson(response, 404, { error: "Endpoint не найден" });
});

server.listen(port, "0.0.0.0", () => {
  console.log(`vulnerable-sqli-demo listening on ${port}`);
});
