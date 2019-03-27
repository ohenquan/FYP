
import pyrebase
import hmac
import codecs
import hashlib
import base64
import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import parse_qs
import logging
from io import BytesIO


bike_password = None
currentDT = datetime.datetime.now()
print("==========Starting Server===========")

def hash_function(deviceAdd, hashVal):
	# Get current datetime

	config = {"apiKey": "AIzaSyBbcWXRQfYmiVQ90Bgmtk3PYCANCnql0QA",
	"authDomain": "fyp4-68372.firebaseapp.com",
	"databaseURL": "https://fyp4-68372.firebaseio.com", 
	"storageBucket": "fyp4-68372.appspot.com",  
	"serviceAccount": "fyp4-68372-firebase-adminsdk-5y8wv-2f2a72fb9a.json"
	}
	username="test@gmail.com"
	password="12345678"

	firebase = pyrebase.initialize_app(config)
	auth = firebase.auth() 

	#authenticate a user > descobrir como não deixar hardcoded
	user = auth.sign_in_with_email_and_password(username, password)

	#user['idToken']
	# At pyrebase'sgit the author said the token expires every 1 hour, so it's needed to refresh it
	user = auth.refresh(user['refreshToken'])
	userIdToken = user['idToken']

	#set database
	db = firebase.database()
	current_key = None
	#and now the hard/ easy part that took me a while to figure out:
	# notice the value inside the .child, it should be the parent name with all the cats keys
	keyval = db.child('parking_key').get()
	for val in keyval.each():
		if (val.key() == deviceAdd):
			theKey = val.val()
			for key,vals in theKey.items():
				current_key = vals

	currentDT = datetime.datetime.now()
	# Add device address with current date and time
	combinedHash = deviceAdd + str(currentDT.year) + str(currentDT.month) + str(currentDT.day) + str(currentDT.hour) + current_key

	# hashing with SHA256
	hash_object = hashlib.sha256(combinedHash.encode())
	hex_dig = hash_object.hexdigest()


	values = db.child('parking_lot').get()
	hello = "Incorrect"
	for val in values.each():
		print("hex val:",hashVal)
		print("hexdig:", hex_dig)
		print("deviceAddress:",deviceAdd)
		if(val.val().replace("_",":") == deviceAdd and hex_dig == hashVal):
			print("reach here to send correct")
			hello = "Correct"
			# Add the number for password num
	return hello


def checkParking(deviceAddress):
	config = {"apiKey": "AIzaSyBbcWXRQfYmiVQ90Bgmtk3PYCANCnql0QA",
	"authDomain": "fyp4-68372.firebaseapp.com",
	"databaseURL": "https://fyp4-68372.firebaseio.com", 
	"storageBucket": "fyp4-68372.appspot.com",  
	"serviceAccount": "fyp4-68372-firebase-adminsdk-5y8wv-2f2a72fb9a.json"
	}
	username="test@gmail.com"
	password="12345678"

	firebase = pyrebase.initialize_app(config)
	auth = firebase.auth() 

	#authenticate a user > descobrir como não deixar hardcoded
	user = auth.sign_in_with_email_and_password(username, password)

	#user['idToken']
	# At pyrebase'sgit the author said the token expires every 1 hour, so it's needed to refresh it
	user = auth.refresh(user['refreshToken'])
	userIdToken = user['idToken']

	#set database
	db = firebase.database()
	values = db.child('parking_lot').get()
	valid = "Invalid"
	for val in values.each():
		print("replaced:",val.val().replace(":","_"))
		print("deviceaddress:",deviceAddress)
		if(val.val().replace(":","_") == deviceAddress):
			valid = "Valid"
			print("Comparing:",val.val(),", With:",deviceAddress)
	print("valid is:",valid)
	return valid



def getPassword(id_db):
	config = {"apiKey": "AIzaSyBbcWXRQfYmiVQ90Bgmtk3PYCANCnql0QA",
	"authDomain": "fyp4-68372.firebaseapp.com",
	"databaseURL": "https://fyp4-68372.firebaseio.com", 
	"storageBucket": "fyp4-68372.appspot.com",  
	"serviceAccount": "fyp4-68372-firebase-adminsdk-5y8wv-2f2a72fb9a.json"
	}
	# add a way to encrypt those, I'm a starter myself and don't know how
	username="test@gmail.com"
	password="12345678"

	firebase = pyrebase.initialize_app(config)
	auth = firebase.auth() 

	#authenticate a user > descobrir como não deixar hardcoded
	user = auth.sign_in_with_email_and_password(username, password)

	#user['idToken']
	# At pyrebase'sgit the author said the token expires every 1 hour, so it's needed to refresh it
	user = auth.refresh(user['refreshToken'])
	userIdToken = user['idToken']

	#set database
	db = firebase.database()
	values = db.child('Unlock').get()
	hello = None
	pw = None
	# In each of the bike id, for loop it
	for val in values.each():
		if (val.key()==id_db):
			hello = val.val()
			for key,val in hello.items():
				if (key == 'pwArray'):
					thelist = val
					thelist.pop(0)
					thestring = ''.join(str(x)+":" for x in thelist)
				if(key == 'password_num'):
					pw = val
	return str(pw)+':'+thestring



class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        # getPassword(123)
        self.wfile.write(b's')

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        body = self.rfile.read(content_length)
        self.send_response(200)
        self.end_headers()
        response = BytesIO()
        lenWholeMessage = len(body.decode('utf-8')) - 1
        # response.write(b'This is POST request. ')
        # response.write(b'Received: ')


        if("checkParking" in body.decode('utf-8')):
            title, theId = body.decode('utf-8').split('=',lenWholeMessage)
            print("i got this address:",theId[:-1])
            response.write(checkParking(theId[:-1]).encode())
            self.wfile.write(response.getvalue())
            print(response.getvalue(),"<what i sent back")

        if("setHash" in body.decode('utf-8')):
            print(body.decode('utf-8'))
            one, hashVal, deviceAdd = body.decode('utf-8').split('=',lenWholeMessage)
            print(one)
            print(hashVal)
            print(deviceAdd)
            response.write(hash_function(deviceAdd[:-1].replace('_',":"), hashVal[:-1]).encode())
            self.wfile.write(response.getvalue())

        if("getPassword" in body.decode('utf-8')):
            title, theId = body.decode('utf-8').split('=',lenWholeMessage)
            print("hello",theId[:-1])
            response.write(getPassword(theId[:-1]).encode())
            self.wfile.write(response.getvalue())
            # lenWholePassword = len(body.decode('utf-8')) - 1
            # title, passw = body.decode('utf-8').split('=',lenWholePassword)
            # print(passw[:-1])

if __name__ == '__main__':
    httpd = HTTPServer(('172.20.10.3', 8000), 
    SimpleHTTPRequestHandler)
    httpd.serve_forever()

# JL place(192.168.1.119)
# School(172.17.192.93)