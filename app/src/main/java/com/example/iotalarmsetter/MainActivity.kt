package com.example.iotalarmsetter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.widget.NumberPicker
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.android.synthetic.main.activity_main.*
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import java.time.LocalTime
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity() {
    //サーバーとの通信に使用する
    data class Setting(
        var alarm: Boolean,
        var hour: Int,
        var minute: Int,
        var secondary_alarm:Boolean,
        var secondary_alarm_offset:Int
    )
    private fun stopToSend(){
        send_setting.visibility = VISIBLE
        send_stop.visibility = View.INVISIBLE
    }
    private fun uiBehaviorSet(){

        simpleTimePicker.setIs24HourView(true)
        simpleTimePicker.setOnTimeChangedListener { _, _,_ ->
            stopToSend()
            time_left_reload()
        }

        togglePlaimaryAlart.setOnClickListener {
            stopToSend()
        }

        secondaryAlarmOffset.maxValue = 90
        secondaryAlarmOffset.minValue = 0
        secondaryAlarmOffset.wrapSelectorWheel =false
        secondaryAlarmOffset.setOnValueChangedListener { numberPicker: NumberPicker, i: Int, i1: Int ->
            time_left_reload()
        }

        secondaryAlarmOffsetIsPlus.setOnCheckedChangeListener { compoundButton, b ->
            time_left_reload()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //theme_related()
        Log.d("NightMode","${AppCompatDelegate.getDefaultNightMode()}")
        setContentView(R.layout.activity_main)

            //設定の読み込み（古）

        /*var alarm_setting = getSharedPreferences("Alarm", MODE_PRIVATE)
        simpleTimePicker.hour = alarm_setting.getInt("hour",7)
        simpleTimePicker.minute = alarm_setting.getInt("minute",0)
        toggle_alart.setChecked(
            alarm_setting.getBoolean("ON/OFF",false)
        )*/
        //設定の読み込み（新）
        setting_load()
        //停止ボタン
        send_stop.setOnClickListener {
            "http://192.168.0.141:10458/alarm/stop".httpGet().response {request, response, result ->

                when (result) {
                    is Result.Success -> {
                        // レスポンスボディを表示
                        debug_text.text = "停止"
                    }
                    is Result.Failure -> {
                        debug_text.text = "停止に失敗しました。"
                    }
                }

            }
        }

        send_setting.setOnClickListener{
            debug_text.text = "button pushed!!"
            val alarm = togglePlaimaryAlart.isChecked
            val hour = simpleTimePicker.hour
            val minute = simpleTimePicker.minute
            val secondary_offset = offset_parse()
            val setting =Setting(togglePlaimaryAlart.isChecked,
                                 simpleTimePicker.hour,
                                 simpleTimePicker.minute,
                                 toggleSecondaryAlart.isChecked,
                                 offset_parse()
            )
            //設定をサーバーに送信
            val gson = Gson()
            val bodyJson = gson.toJson(setting)
                "http://192.168.0.141:10458/alarm/set".httpPut()
                    .header("Content-Type" to "application/json")
                    .body(bodyJson)
                    .response{request, response, result ->
                        when (result) {
                            is Result.Success -> {
                                // レスポンスボディを表示
                                debug_text.text = "設定を送信しました。"
                                send_setting.visibility = View.INVISIBLE
                                send_stop.visibility = VISIBLE
                            }
                            is Result.Failure -> {
                                debug_text.text = "通信に失敗しました。"
                            }
                        }
                    }

            }



    }

    override fun onResume() {
        super.onResume()
        time_left_reload()
    }

    fun offset_parse():Int{
          when(secondaryAlarmOffsetIsPlus.isChecked){
            true ->{
                return secondaryAlarmOffset.getValue()
            }
            false ->{
                return -(secondaryAlarmOffset.getValue())
            }
        }
    }
    //アラームが鳴るまでの時間を表示する
    fun time_left_reload(){
        //primary
        val now = LocalTime.now()
        val target = LocalTime.of(simpleTimePicker.hour,simpleTimePicker.minute)
        var date = target.minusHours(now.hour.toLong())
                         .minusMinutes(now.minute.toLong())
        primaryAlarmLeft.text = "Primary ${date.hour}:${date.minute} time left"
        //secondary
        val secondry = date.plusMinutes(offset_parse().toLong())
        secondryAlarnLeft.text = "Secondry ${secondry.hour}:${secondry.minute} time left"
    }

    fun setting_load(){
        //Async
        "http://192.168.0.141:10458/alarm/get".httpGet().responseString { _, _, result ->
            when(result){
                is Result.Failure ->{
                    debug_text.text = "設定の読み込みに失敗しました。"
                }
                is Result.Success ->{
                    val gson = Gson()
                    var data = gson.fromJson(result.value, Setting::class.java)
                    //設定をサーバーから受信
                    togglePlaimaryAlart.isChecked = data.alarm
                    simpleTimePicker.hour = data.hour
                    simpleTimePicker.minute = data.minute
                    toggleSecondaryAlart.isChecked = data.secondary_alarm
                    secondaryAlarmOffset.value = data.secondary_alarm_offset.absoluteValue
                    secondaryAlarmOffsetIsPlus.isChecked = if (data.secondary_alarm_offset >= 0) true else false
                    debug_text.text = "設定の読み込みに成功しました。"
                }
            }
            //並列でUIの設定をする
            uiBehaviorSet()
            time_left_reload()
        }}

    }


