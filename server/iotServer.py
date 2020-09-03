#!/usr/bin/env python3


# Flask などの必要なライブラリをインポートする
from time import sleep
from flask import Flask, render_template, request, redirect, url_for, jsonify
import json
import threading
import datetime
import RPi.GPIO as GPIO
# 自身Flask(__name__)
app = Flask(__name__)


alarm_thread_bool = False
alarm_thread_hour = 99
alarm_thread_minute = 99
secondary_alarm = False
secondary_alarm_offset = 0

#MOUOKITA = False


@app.route('/alarm/set', methods=['PUT'])
def alarm_set():
    req = request.get_json()
    if request.headers['Content-Type'] == 'application/json':
        try:
            global alarm_thread_bool
            global alarm_thread_hour
            global alarm_thread_minute
            global secondary_alarm
            global secondary_alarm_offset
            alarm_thread_bool = req['alarm']
            alarm_thread_hour = req['hour']
            alarm_thread_minute = req['minute']
            secondary_alarm = req['secondary_alarm']
            secondary_alarm_offset = req['secondary_alarm_offset']

            if alarm_thread_bool == True:
                print("TrueTrueTrueTrueTrueTrue")
                return jsonify(result={"status": 200})
            if alarm_thread_bool == False:
                print("elesleslelsllslellslslslsessseleslseesleslessel")
                return jsonify(result={"status": 200})
            return jsonify(result={"status": 500, "debug": alarm_thread_bool})
        except:
            print("Err")
            return jsonify(result={"status": 400})


def alarm_deamon():
    while True:
        sleep(1)
        if alarm_thread_bool == True:
            dt_now = datetime.datetime.now()
            if dt_now.hour == alarm_thread_hour and dt_now.minute == alarm_thread_minute:
              #              if MOUOKITA == True:
              #                  MOUOKITA = False
              #                  continue
                # ここに鳴らす処理を書く
                GPIO.output(ALARM_PIN, True)
                print("!!!!!!")
                sleep(60)


ALARM_PIN = 14
GPIO.setmode(GPIO.BCM)
GPIO.setup(ALARM_PIN, GPIO.OUT)


@app.route('/alarm/stop')
def alarm_stop():
    GPIO.output(ALARM_PIN, False)
    return jsonify(result={"status": 200})


@app.route('/alarm/get')
def alarm_get():
    global alarm_thread_bool
    global alarm_thread_hour
    global alarm_thread_minute
    return jsonify({"alarm": alarm_thread_bool,
                    "hour": alarm_thread_hour,
                    "minute": alarm_thread_minute,
                    "secondary_alarm": secondary_alarm,
                    "secondary_alarm_offset": secondary_alarm_offset
                    })


if __name__ == '__main__':
    #    app.debug = True # デバッグモード有効化
    alm = threading.Thread(target=alarm_deamon)
    alm.setDaemon(True)
    alm.start()
    app.run(host='192.168.0.141', port='10458')
    GPIO.cleanup(ALARM_PIN)
