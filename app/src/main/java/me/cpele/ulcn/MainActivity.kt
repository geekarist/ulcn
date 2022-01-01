package me.cpele.ulcn

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.set
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.text_view)
        textView.text = intent.let {
            val uri = it.data
            val encodedUri = uri.toString()
            val decodedUri = URLDecoder.decode(encodedUri)
            val q = decodedUri.replace("geo:0,0?q=", "")
            val links = q.split(' ')
            val spans = links.map { link: CharSequence ->
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        startActivity(
                            Intent()
                                .setAction(Intent.ACTION_VIEW)
                                .setData(Uri.parse(link.toString()))
                        )
                    }
                } to link
            }.map { (span, link) ->
                SpannableStringBuilder(link).apply {
                    setSpan(
                        span,
                        0,
                        link.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }.map { builder ->
                builder.apply {
                    appendln()
                    appendln()
                }
            }
            TextUtils.concat(*spans.toTypedArray())
        }
    }
}