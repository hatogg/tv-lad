package ua.x44to.lad

import android.app.Activity
import android.media.tv.TvContract
import android.media.tv.TvView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast

/**
 * Запасний спосіб показати вхід HDMI: власний повноекранний TvView.
 * Потрібен там, де системна програма ТВ не відкривається на потрібному вході.
 * «Назад» чи «Додому» на пульті — повернення на головний екран.
 */
class InputActivity : Activity() {
    private var tv: TvView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val id = intent.getStringExtra(EXTRA_ID)
        if (id == null) {
            finish()
            return
        }
        val view = TvView(this)
        view.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContentView(view)
        // кнопки пульта йдуть у сам вхід (TvView передає їх далі, напр. для HDMI-CEC); «Назад» — повернення
        view.isFocusable = true
        view.requestFocus()
        tv = view
        view.setCallback(object : TvView.TvInputCallback() {
            override fun onConnectionFailed(inputId: String) {
                Toast.makeText(this@InputActivity, "Не вдалося відкрити вхід", Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onDisconnected(inputId: String) {
                finish()
            }
        })
        try {
            view.tune(id, TvContract.buildChannelUriForPassthroughInput(id))
        } catch (e: Exception) {
            Toast.makeText(this, "Телевізор не дозволяє показати цей вхід", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        tv?.reset()
        finish()
    }

    companion object {
        const val EXTRA_ID = "input_id"
    }
}
