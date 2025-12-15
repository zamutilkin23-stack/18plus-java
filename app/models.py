from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import Column, Integer, String, Text

db = SQLAlchemy()

class Card(db.Model):
    __tablename__ = 'cards'
    id = Column(Integer, primary_key=True)
    text = Column(Text, nullable=False)
    level = Column(Integer, nullable=False)
    orientation = Column(String(10), nullable=False)

    def to_dict(self):
        return {
            'id': self.id,
            'text': self.text,
            'level': self.level,
            'orientation': self.orientation
        }

class Player(db.Model):
    __tablename__ = 'players'
    id = Column(Integer, primary_key=True)
    name = Column(String(50), nullable=False)
    gender = Column(String(1), nullable=False)
    orientation = Column(String(10), nullable=False)
    game_id = Column(Integer, nullable=False)