package com.example.iotalarmsetter

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.widget.TimePicker
import com.github.kittinunf.fuel.Fuel
import kotlinx.android.synthetic.main.activity_main.*
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result

import com.squareup.moshi.Moshi


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        simpleTimePicker.setIs24HourView(true)


        //設定の読み込み
        var alarm_setting = getSharedPreferences("Alarm", MODE_PRIVATE)
        simpleTimePicker.hour = alarm_setting.getInt("hour",7)
        simpleTimePicker.minute = alarm_setting.getInt("minute",0)
        toggle_alart.setChecked(
            alarm_setting.getBoolean("ON/OFF",false)
        )

        //停止ボタン
        send_stop.setOnClickListener {
            "http://192.168.0.141:10458/alarm/stop".httpGet().response {request, response, result ->

                when (result) {
                    is Result.Success -> {
                        // レスポンスボディを表示
                        debug_text.setText("停止")
                    }
                    is Result.Failure -> {
                        debug_text.setText("停止に失敗しました。")
                    }
                }

            }
        }
        //設定を変更したら送信ボタンが出現
        simpleTimePicker.setOnTimeChangedListener { _, _,_ ->
            send_setting.visibility = VISIBLE
            send_stop.visibility = View.INVISIBLE
        }
        toggle_alart.setOnClickListener {
            send_setting.visibility = VISIBLE
            send_stop.visibility = View.INVISIBLE
        }

        send_setting.setOnClickListener{
            debug_text.setText("button pushed!!")
            val alarm = toggle_alart.isChecked
            val hour = simpleTimePicker.hour
            val minute = simpleTimePicker.minute


            /*"http://192.168.0.141:10458/test".httpGet().response {request, response, result ->

                when (result) {
                    is Result.Success -> {
                        // レスポンスボディを表示
                        debug_text.setText("通信しました。")
                    }
                    is Result.Failure -> {
                        debug_text.setText("通信に失敗しました。")
                    }
                }

            };*/
            //設定をサーバーに送信
                val bodyJson = """{"alarm": $alarm,"hour": $hour,"minute": $minute}"""
                "http://192.168.0.141:10458/alarm/set".httpPut()
                    .header("Content-Type" to "application/json")
                    .body(bodyJson)
                    .response{request, response, result ->
                        when (result) {
                            is Result.Success -> {
                                // レスポンスボディを表示
                                debug_text.setText("設定を送信しました。")
                                send_setting.visibility = View.INVISIBLE
                                send_stop.visibility = VISIBLE
                            }
                            is Result.Failure -> {
                                debug_text.setText("通信に失敗しました。")
                            }
                        }
                    }
                //保存処理
                setting_save(alarm,hour,minute)
            }



    }

    fun setting_save (b: Boolean,h:Int,m:Int){
        var alarm_setting = getSharedPreferences("Alarm", MODE_PRIVATE)
        var editer = alarm_setting.edit()
        editer.putBoolean("ON/OFF",b)
        editer.putInt("hour",h)
        editer.putInt("minute",m)
        editer.commit()
        //debug_text.setText("setting_saved!!")
    }


}
