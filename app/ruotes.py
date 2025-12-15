from flask import Blueprint, render_template, request, redirect, url_for, session, jsonify
from .models import db, Card, Player

main = Blueprint('main', __name__)

ADMIN_USER = "admin"
ADMIN_PASS = "admin123"

@main.route('/')
def index():
    return render_template('index.html')

@main.route('/age-gate')
def age_gate():
    return render_template('age_gate.html')

@main.route('/check-age', methods=['POST'])
def check_age():
    year = int(request.form['year'])
    if 2025 - year >= 18:
        session['age_verified'] = True
        return redirect(url_for('players_page'))
    return '<h1 style="color:#ff0066;text-align:center;">❌ Вам нет 18 лет!</h1>'

@main.route('/players')
def players_page():
    if not session.get('age_verified'):
        return redirect('/age-gate')
    return render_template('players.html')

@main.route('/api/start-game', methods=['POST'])
def start_game():
    Player.query.filter_by(game_id=1).delete()
    db.session.commit()

    players_data = request.json
    for p in players_data:
        player = Player(
            name=p['name'],
            gender=p['gender'],
            orientation=p['orientation'],
            game_id=1
        )
        db.session.add(player)
    db.session.commit()
    return jsonify(ok=True)

@main.route('/board')
def board():
    if not session.get('age_verified'):
        return redirect('/age-gate')
    players = Player.query.filter_by(game_id=1).all()
    if len(players) < 2:
        return redirect('/players')
    return render_template('board.html', players=players)

@main.route('/api/draw-card')
def draw_card():
    from collections import Counter
    import random

    players = Player.query.filter_by(game_id=1).all()
    if not players:
        return jsonify(text="❌ Нет игроков", level=0)

    group_gender = ''.join(p.gender for p in players)
    group_counter = Counter(group_gender.upper())

    def is_compatible(required: str) -> bool:
        if required == "любая":
            return True
        need = Counter(required.upper())
        return all(group_counter[char] >= need[char] for char in need)

    available = [card for card in Card.query.all() if is_compatible(card.orientation)]

    if not available:
        return jsonify(
            text="🔒 Нет карточек, совместимых с составом игроков",
            level=0
        )

    card = random.choice(available)
    return jsonify(text=card.text, level=card.level)

@main.route('/admin/login')
def admin_login():
    return render_template('admin/login.html')

@main.route('/admin/do-login', methods=['POST'])
def do_login():
    user = request.form['user']
    pwd = request.form['pass']
    if user == ADMIN_USER and pwd == ADMIN_PASS:
        session['admin'] = True
        return redirect('/admin')
    return '<p>❌ Ошибка</p><a href="/admin/login">Назад</a>'

@main.route('/admin')
def admin():
    if not session.get('admin'):
        return redirect('/admin/login')
    cards = Card.query.all()
    return render_template('admin/index.html', cards=cards)

@main.route('/admin/add', methods=['GET', 'POST'])
def add_card():
    if not session.get('admin'):
        return redirect('/admin/login')
    if request.method == 'POST':
        card = Card(
            text=request.form['text'],
            level=int(request.form['level']),
            orientation=request.form['orientation']
        )
        db.session.add(card)
        db.session.commit()
        return redirect('/admin')
    return render_template('admin/add.html')

@main.route('/admin/delete/<int:card_id>')
def delete_card(card_id):
    if not session.get('admin'):
        return redirect('/admin/login')
    card = Card.query.get_or_404(card_id)
    db.session.delete(card)
    db.session.commit()
    return redirect('/admin')