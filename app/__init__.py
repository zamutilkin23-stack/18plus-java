from flask import Flask
from app.models import db

def create_app():
    app = Flask(__name__)
    app.secret_key = 'supersecretkey'

    import os
    from dotenv import load_dotenv
    load_dotenv()
    app.config['SQLALCHEMY_DATABASE_URI'] = os.getenv('DATABASE_URL')
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

    db.init_app(app)

    with app.app_context():
        db.create_all()
        from .models import Card
        if Card.query.count() == 0:
            db.session.add_all([
                Card(text="Сделайте комплимент игроку слева", level=1, orientation="любая"),
                Card(text="Поцелуйте игрока напротив", level=3, orientation="МЖ"),
                Card(text="Расскажите о своей самой смелой фантазии", level=4, orientation="ММ"),
                Card(text="Покажите, как вы любите целоваться", level=2, orientation="ЖЖ"),
                Card(text="Задайте провокационный вопрос", level=2, orientation="любая"),
                Card(text="Мужчины, поцелуйтесь", level=4, orientation="ММ"),
                Card(text="Девушки, обнимитесь", level=3, orientation="ЖЖ"),
            ])
            db.session.commit()

    from .routes import main
    app.register_blueprint(main)

    return app