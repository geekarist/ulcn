package me.cpele.ulcn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.URLDecoder

class MainActivity : AppCompatActivity() {
    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val result = textViewContents(this, intent)
        val headerTextView = findViewById<TextView>(R.id.main_links_found)
        headerTextView.text = getString(R.string.main_links_found)
        val textView = findViewById<TextView>(R.id.main_result)
        textView.text = result
    }
}

fun textViewContents(
    context: Context,
    actIntent: Intent
): CharSequence {

    val uri = actIntent.data
    val encodedUri = uri.toString()

    @Suppress("DEPRECATION")
    val decodedUri = URLDecoder.decode(encodedUri)
    val q = decodedUri.replace("geo:0,0?q=", "")
    val words = q.split(' ')

    val spans = words.asSequence()
        .map { link -> convertToIntent(link) }
        .filter { intent -> isIntentManaged(intent, context.packageManager) }
        .map { intent ->
            val span = clickableSpan(context, intent)
            span to intent
        }.map { (span, intent) ->
            applySpanToIntent(span, intent)
        }.toList()

    val arrayOfSpans = spans.toTypedArray()
    val result = TextUtils.concat(*arrayOfSpans)

    fun getFallback() = context.getString(R.string.main_empty_spans_fallback)
    fun isResultBlank() = result.isNullOrBlank()
    return if (isResultBlank()) getFallback() else result
}

fun convertToIntent(link: String) =
    Intent()
        .setAction(Intent.ACTION_VIEW)
        .setData(Uri.parse(link))

@SuppressLint("QueryPermissionsNeeded")
fun isIntentManaged(intent: Intent, packageManager: PackageManager) =
    packageManager
        .queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .isNotEmpty()

fun clickableSpan(context: Context, intent: Intent): ClickableSpan {

    fun openIntent() = context.startActivity(intent)
    fun fallbackReasonMsg() = context.getString(R.string.main_reason_unknown)
    fun reasonMsg(e: Exception) = e.message ?: fallbackReasonMsg()
    fun errorMsg(e: Exception) = context.getString(
        R.string.main_error_opening_link,
        reasonMsg(e)
    )

    fun showError(e: Exception) = Toast.makeText(
        context,
        errorMsg(e),
        Toast.LENGTH_LONG
    ).show()

    return object : ClickableSpan() {
        override fun onClick(widget: View) {
            try {
                openIntent()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }
}

@Suppress("DEPRECATION")
fun applySpanToIntent(
    span: Any,
    intent: Intent
): CharSequence =
    SpannableStringBuilder(intent.data.toString())
        .apply {
            setSpan(
                span,
                0,
                intent.data.toString().length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            appendln()
            appendln()
        }

