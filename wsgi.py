import sys
print(f"[Чаки] Запущено с Python: {sys.version}")
print(f"[Чаки] Версия: {sys.version_info}")

from app import create_app

app = create_app()

if __name__ == '__main__':
    print("[Чаки] Сервер запущен на порту 7000")
    app.run(host='0.0.0.0', port=7000, debug=False)