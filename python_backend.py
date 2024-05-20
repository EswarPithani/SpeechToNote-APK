import os
from flask import Flask, request, jsonify, session, send_from_directory
from flask_bcrypt import Bcrypt
from flask_mysqldb import MySQL
import secrets
import logging

app = Flask(__name__)
app.secret_key = secrets.token_hex(16)
bcrypt = Bcrypt(app)

app.logger.setLevel(logging.INFO)

app.config['MYSQL_HOST'] = '127.0.0.1'
app.config['MYSQL_PORT'] = 3306
app.config['MYSQL_USER'] = 'root'
app.config['MYSQL_PASSWORD'] = ''
app.config['MYSQL_DB'] = ''
mysql = MySQL(app)

UPLOAD_FOLDER = 'uploads'
ALLOWED_EXTENSIONS = {'pdf', 'txt', 'doc', 'docx'}

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER


@app.route('/register', methods=['POST'])
def register():
    try:
        username = request.json.get('username')
        email = request.json.get('email')
        password = request.json.get('password')
        role = request.json.get('role')

        if not all([username, email, password, role]):
            app.logger.error('Missing required fields in registration data')
            return jsonify({'message': 'Missing required fields in registration data'}), 400

        cursor = mysql.connection.cursor()
        cursor.execute("SELECT COUNT(*) FROM user WHERE username = %s", (username,))
        count = cursor.fetchone()[0]
        if count > 0:
            cursor.close()
            return jsonify({'message': 'Username already exists'}), 400

        hashed_password = bcrypt.generate_password_hash(password).decode('utf-8')
        cursor.execute("INSERT INTO user (username, email, password, role) VALUES (%s, %s, %s, %s)",
                       (username, email, hashed_password, role))
        user_id = cursor.lastrowid
        if role.lower() == 'admin':
            cursor.execute("INSERT INTO admin (user_id) VALUES (%s)", (user_id,))
        elif role.lower() == 'teacher':
            cursor.execute("INSERT INTO teacher (user_id) VALUES (%s)", (user_id,))
        elif role.lower() == 'student':
            cursor.execute("INSERT INTO student (user_id) VALUES (%s)", (user_id,))
        mysql.connection.commit()
        cursor.close()

        return jsonify({'message': 'User registered successfully'}), 201

    except Exception as e:
        app.logger.error(f'Error during registration: {e}')
        return jsonify({'message': 'An error occurred during registration'}), 500


def get_user_info(username, role):
    try:
        conn = mysql.connection
        cursor = conn.cursor()
        cursor.execute(f'SELECT * FROM {role} WHERE username = %s', (username,))
        user = cursor.fetchone()
        cursor.close()
        return user
    except Exception as e:
        app.logger.error(f"Error getting user info: {e}")
        return None


def authenticate_user(username, password):
    try:
        cursor = mysql.connection.cursor()
        cursor.execute("SELECT user_id, password, role FROM user WHERE username = %s", (username,))
        user = cursor.fetchone()
        cursor.close()
        if user and bcrypt.check_password_hash(user[1], password):
            return user[0], user[2] 
    except Exception as e:
        app.logger.error(f"Error while authenticating user: {e}")
    return None, None  


def hash_password(password):
    return bcrypt.generate_password_hash(password).decode('utf-8')

def verify_password(hashed_password, password):
    return bcrypt.check_password_hash(hashed_password, password)

@app.route('/login', methods=['POST'])
def login():
    try:
        username = request.json.get('username')
        password = request.json.get('password')

        if not username or not password:
            return jsonify({'message': 'Username and password are required'}), 400

        user_id, role = authenticate_user(username, password)
        if user_id:
            session['username'] = username
            session['user_id'] = user_id
            fetched_role = get_user_role(user_id)
            if fetched_role:
                session['role'] = fetched_role
            else:
                session['role'] = None
            return jsonify({'message': 'Login successful', 'role': session['role']}), 200
        else:
            return jsonify({'message': 'Incorrect username or password'}), 401

    except Exception as e:
        app.logger.error(f'Error during login: {e}')
        return jsonify({'message': 'An error occurred during login'}), 500

def get_user_info(username):
    try:
        conn = mysql.connection
        cursor = conn.cursor()
        cursor.execute("SELECT username, email, role FROM user WHERE username = %s", (username,))
        user_info = cursor.fetchone()
        cursor.close()
        return user_info
    except Exception as e:
        app.logger.error(f"Error getting user info: {e}")
        return None


@app.route('/profile', methods=['GET'])
def get_profile_info():
    logged_in_username = session.get('username') 

    if logged_in_username:
        cursor = mysql.connection.cursor()
        cursor.execute("SELECT username, email, role FROM user WHERE username = %s", (logged_in_username,))
        user_info = cursor.fetchone()
        cursor.close()

        if user_info:
            user_data = {
                'username': user_info[0],
                'email': user_info[1],
                'role': user_info[2]
            }
            print(f"Retrieved user information: {user_data}")
            return jsonify(user_data), 200
        else:
            return jsonify({'message': 'User not found'}), 404
    else:
        return jsonify({'message': 'User not logged in'}), 401


@app.route('/files', methods=['GET'])
def get_files():
    try:
        cursor = mysql.connection.cursor()
        cursor.execute("SELECT * FROM uploaded_files")
        files = cursor.fetchall()
        cursor.close()

        files_list = []
        for file in files:
            file_dict = {
                'filename': file[1],
                'filepath': file[2]
            }
            files_list.append(file_dict)

        return jsonify({'files': files_list}), 200

    except Exception as e:
        app.logger.error(f'Error fetching files: {e}')
        return jsonify({'message': 'An error occurred while fetching files'}), 500


def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


def get_user_role(user_id):
    try:
        cursor = mysql.connection.cursor()
        cursor.execute("SELECT * FROM admin WHERE user_id = %s", (user_id,))
        if cursor.fetchone():
            return 'admin'
        cursor.execute("SELECT * FROM teacher WHERE user_id = %s", (user_id,))
        if cursor.fetchone():
            return 'teacher'
        cursor.execute("SELECT * FROM student WHERE user_id = %s", (user_id,))
        if cursor.fetchone():
            return 'student'
    except Exception as e:
        app.logger.error(f"Error getting user role: {e}")
    return None

if __name__ == '__main__':
    app.run(host='', port=5000)
