package com.example.iotalarmsetter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.android.synthetic.main.activity_main.*
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import java.time.Clock
import java.time.LocalDateTime



class MainActivity : AppCompatActivity() {
    data class Setting(
        var alarm: Boolean,
        var hour: Int,
        var minute: Int
    )

    fun secondary_alarm_set(){
        secondaryAlarmOffset.maxValue = 90
        secondaryAlarmOffset.minValue = 0
        secondaryAlarmOffset.wrapSelectorWheel =false

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //theme_related()
        Log.d("NightMode","${AppCompatDelegate.getDefaultNightMode()}")
        setContentView(R.layout.activity_main)
        simpleTimePicker.setIs24HourView(true)
        secondary_alarm_set()

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
            val alarm = toggle_alart.isChecked
            val hour = simpleTimePicker.hour
            val minute = simpleTimePicker.minute
            //設定をサーバーに送信
                val bodyJson = """{"alarm": $alarm,"hour": $hour,"minute": $minute}"""
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

    //アラームが鳴るまでの時間を表示する
    fun time_left_reload(){
        //primary
        val hour_now = LocalDateTime.now(Clock.systemDefaultZone()).hour
        val minute_now = LocalDateTime.now(Clock.systemDefaultZone()).minute
        val hour_target = simpleTimePicker.hour
        val minute_target = simpleTimePicker.minute
        var minute_left: Int
        var hour_left = 0
        //分の計算
        minute_left = minute_target - minute_now
        if (minute_left < 0){
            hour_left -= 1
            minute_left +=60}
        //時の計算
        hour_left += hour_target - hour_now
        if (hour_left < 0){hour_left += 24}

        primaryAlarmLeft.text = "${hour_left}:${minute_left} time left"
        //以下secondary

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

                    simpleTimePicker.hour = data.hour
                    simpleTimePicker.minute = data.minute
                    toggle_alart.isChecked = data.alarm
                    debug_text.text = "設定の読み込みに成功しました。"
                }
            }        //設定を変更したら送信ボタンが出現し鳴るまでの時間が更新される
            //ここにあるべきではない
            //並列で読み込まれるせいであるべきところに置くとバグります
            simpleTimePicker.setOnTimeChangedListener { _, _,_ ->
                send_setting.visibility = VISIBLE
                send_stop.visibility = View.INVISIBLE
                time_left_reload()
            }
            toggle_alart.setOnClickListener {
                send_setting.visibility = VISIBLE
                send_stop.visibility = View.INVISIBLE
            }
            time_left_reload()
        }}

    }


