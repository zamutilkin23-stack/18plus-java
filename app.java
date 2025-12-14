echo import io.javalin.Javalin;
import io.javalin.http.Context;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class App {

    // 🔥 Переменные из Render (Environment Variables)
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", io.javalin.http.staticfiles.Location.CLASSPATH);
            config.enableDevLogging();
        }).start(7000);

        if (DB_URL == null) {
            System.err.println("[ERROR] Установите DB_URL, DB_USER, DB_PASS в переменных окружения!");
            return;
        }

        initDB();
        if (isFirstRun()) {
            createAdmin();
            createLevels();
        }

        app.before(ctx -> log("[REQUEST] " + ctx.method() + " " + ctx.path()));

        // === Главная ===
        app.get("/", ctx -> ctx.html("""
            <!DOCTYPE html>
            <html>
            <head><title>18+ ZONE</title>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <style>
                body { background: #1e1a2f; color: white; font-family: Arial; text-align: center; padding: 40px; }
                .btn { padding: 16px 30px; margin: 15px; font-size: 1.3em; background: #ff4757; color: white; text-decoration: none; border-radius: 12px; display: inline-block; }
                @media (max-width: 600px) {
                    body { padding: 20px; }
                    .btn { font-size: 1.1em; padding: 12px 20px; }
                }
            </style>
            </head>
            <body>
                <h1>🔞 18+ ZONE</h1>
                <a href='/age-gate' class='btn'>Проверить возраст</a>
            </body>
            </html>
        """));

        // === Проверка возраста ===
        app.get("/age-gate", ctx -> ctx.html("""
            <!DOCTYPE html>
            <html>
            <head><title>Возраст</title>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <style>
                body { background: #1e1a2f; color: white; font-family: Arial; text-align: center; padding: 40px; }
                form { max-width: 400px; margin: auto; }
                input, button { width: 100%%; padding: 14px; margin: 10px 0; font-size: 1.1em; border-radius: 8px; border: none; }
                button { background: #ff4757; color: white; cursor: pointer; }
                a { color: #ff4757; text-decoration: none; }
                @media (max-width: 600px) {
                    input, button { font-size: 1em; padding: 12px; }
                }
            </style>
            </head>
            <body>
                <h1>⚠️ Подтвердите возраст</h1>
                <form method='post' action='/check-age'>
                    <input name='year' type='number' min='1900' max='2025' placeholder='Год рождения' required>
                    <button>Продолжить</button>
                </form>
                <a href='/'>← Назад</a>
            </body>
            </html>
        """));

        app.post("/check-age", ctx -> {
            int year = Integer.parseInt(ctx.formParam("year"));
            if (2025 - year >= 18) {
                ctx.req().getSession(true);
                ctx.redirect("/game");
            } else {
                ctx.html("<h1>❌ Вам нет 18 лет!</h1><a href='/age-gate'>Попробовать снова</a>");
            }
        });

        // === Игра ===
        app.get("/game", ctx -> {
            if (ctx.req().getSession(false) == null) ctx.redirect("/age-gate");
            Card card = getRandomCard();
            if (card == null) {
                ctx.html(getLevelUpPage());
                return;
            }
            Level level = getLevel(card.level);
            ctx.html("""
                <!DOCTYPE html>
                <html>
                <head><title>Карточка</title>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <style>
                    body { background: #1e1a2f; color: white; font-family: Arial; text-align: center; padding: 20px; }
                    .card {
                        width: 90%%; max-width: 500px; padding: 30px; margin: 20px auto; border-radius: 16px;
                        font-size: 1.3em; font-weight: bold; color: """ + (level.color.equals("#9c27b0") || level.color.equals("#f44336") ? "white" : "black") + """;
                        background: """ + level.color + """;
                        box-shadow: 0 4px 15px rgba(0,0,0,0.3);
                    }
                    .level-tag { font-size: 0.9em; color: #ccc; margin-bottom: 15px; display: block; }
                    .btn {
                        padding: 14px 25px; margin: 10px; font-size: 1.1em; border: none; border-radius: 8px;
                        cursor: pointer; text-decoration: none; display: inline-block;
                    }
                    .btn-ok { background: #4CAF50; color: white; }
                    .btn-penalty { background: #f44336; color: white; }
                    .btn:hover { transform: scale(1.05); transition: 0.3s; }
                    .nav { margin-top: 30px; }
                    .nav a { color: #ff4757; text-decoration: none; margin: 0 15px; }
                    @media (max-width: 600px) {
                        .card { font-size: 1.1em; padding: 20px; }
                        .btn { font-size: 1em; padding: 12px 20px; }
                    }
                </style>
                </head>
                <body>
                    <h1>🃏 ИГРА: КАРТОЧКИ</h1>
                    <div class='card'>
                        <span class='level-tag'>""" + level.name + """</span>
                        """ + card.text + """
                    </div>
                    <div>
                        <a href='/game/done?id=""" + card.id + """' class='btn btn-ok'>✅ ВЫПОЛНЕНО</a>
                        <a href='/game/penalty?id=""" + card.id + """' class='btn btn-penalty'>❌ ШТРАФ</a>
                    </div>
                    <div class='nav'>
                        <a href='/admin'>🛠️ Админка</a> | <a href='/'>🏠 Главная</a>
                    </div>
                </body>
                </html>
            """);
        });

        app.get("/game/done", ctx -> {
            if (ctx.req().getSession(false) == null) ctx.redirect("/age-gate");
            int id = Integer.parseInt(ctx.queryParam("id"));
            deleteCard(id);
            log("[GAME] Выполнено: " + id);
            ctx.redirect("/game");
        });

        app.get("/game/penalty", ctx -> {
            if (ctx.req().getSession(false) == null) ctx.redirect("/age-gate");
            int id = Integer.parseInt(ctx.queryParam("id"));
            markAsPenalty(id);
            log("[GAME] Штраф: " + id);
            ctx.redirect("/game");
        });

        // === Админка ===
        app.get("/admin/login", ctx -> ctx.html("""
            <!DOCTYPE html>
            <html><head><title>Админ</title>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <style>
                body { background: #1e1a2f; color: white; font-family: Arial; text-align: center; padding: 40px; }
                input, button { width: 80%%; max-width: 300px; padding: 14px; margin: 10px; font-size: 1.1em; border-radius: 8px; border: none; }
                button { background: #ff4757; color: white; cursor: pointer; }
                a { color: #ff4757; }
            </style>
            </head>
            <body>
                <h1>🔐 Вход в админку</h1>
                <form method='post' action='/admin/login'>
                    <input name='username' placeholder='Логин' required>
                    <input name='password' type='password' placeholder='Пароль' required>
                    <button>Войти</button>
                </form>
                <a href='/'>← На главную</a>
            </body>
            </html>
        """));

        app.post("/admin/login", ctx -> {
            String user = ctx.formParam("username");
            String pass = ctx.formParam("password");
            if (ADMIN_USER.equals(user) && verifyPassword(pass)) {
                ctx.req().getSession(true).setAttribute("admin", true);
                log("[ADMIN] Вход: " + user);
                ctx.redirect("/admin");
            } else {
                log("[ADMIN] Ошибка: " + user);
                ctx.html("<p style='color:red'>❌ Неверный логин или пароль</p><a href='/admin/login'>Повторить</a>");
            }
        });

        app.get("/admin", ctx -> {
            if (ctx.req().getSession(false) == null || ctx.req().getSession().getAttribute("admin") == null) {
                ctx.redirect("/admin/login");
            }
            List<Card> cards = getCards();
            List<Level> levels = getLevels();
            StringBuilder list = new StringBuilder();
            list.append("<!DOCTYPE html><html><head><title>Админ</title>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<style>body{background:#1e1a2f;color:white;font-family:Arial}a{color:#ff4757}")
                .append("table{width:100%%;border-collapse:collapse;margin:20px 0}th,td{padding:12px;text-align:left;border-bottom:1px solid #333}")
                .append("tr:hover{background:#2d2d2f}.btn{padding:8px 16px;background:#ff4757;color:white;text-decoration:none;border-radius:6px}</style>")
                .append("</head><body><div style='max-width:1000px;margin:auto;padding:20px'>")
                .append("<h1>🛡️ Админ-панель</h1>")
                .append("<a href='/admin/add-card' class='btn'>➕ Добавить карточку</a><hr>")
                .append("<table><tr><th>Текст</th><th>Уровень</th><th>Цвет</th><th>Действия</th></tr>");

            for (Card card : cards) {
                Level level = levels.stream().filter(l -> l.id == card.level).findFirst().orElse(new Level(0, "Ошибка", "#ff0000"));
                list.append("<tr>")
                    .append("<td>").append(card.text).append("</td>")
                    .append("<td>").append(level.name).append("</td>")
                    .append("<td style='background:").append(level.color).append(";color:").append((level.color.equals("#9c27b0") || level.color.equals("#f44336") ? "white" : "black"))
                    .append(";padding:5px 10px;border-radius:5px;'>Цвет</td>")
                    .append("<td>")
                    .append("<a href='/admin/edit-card?id=").append(card.id).append("' class='btn'>✏️</a> ")
                    .append("<a href='/admin/delete-card?id=").append(card.id).append("' class='btn' style='background:#ff3742'>🗑️</a>")
                    .append("</td></tr>");
            }

            list.append("</table><hr>")
                .append("<a href='/game' class='btn'>🃏 К игре</a> | ")
                .append("<a href='/admin/logout' style='color:#ffcc00'>Выход</a>")
                .append("</div></body></html>");

            ctx.html(list.toString());
        });

        // --- Остальные маршруты админки: add, edit, delete, logout ---
        // (аналогично предыдущим версиям — добавляются по необходимости)

        // === API ===
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "ok")));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> log("[SHUTDOWN] Сервер остановлен.")));
    }

    // --- Все методы: initDB, isFirstRun, createAdmin, verifyPassword, CRUD, log, Card, Level ---
    // (реализованы как в предыдущих версиях с PostgreSQL)

    private static void log(String msg) {
        String time = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[LOG] " + time + " | " + msg);
        try {
            Files.write(Paths.get("server.log"), (time + " | " + msg + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) { e.printStackTrace(); }
    }

    static class Card {
        int id;
        String text;
        int level;
        Card(int id, String text, int level) {
            this.id = id;
            this.text = text;
            this.level = level;
        }
    }

    static class Level {
        int id;
        String name;
        String color;
        Level(int id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
    }
} > "D:\repozit\18+\src\App.java"